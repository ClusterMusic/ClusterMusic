package model

import slick.jdbc.MySQLProfile.api._

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random


case class Listen(timestamp: Timestamp)


abstract class Radio(val viewer: Identity[User]) {
  val listens: mutable.Map[Identity[Song], Listen] = mutable.Map()
  var buffer = Seq.empty[PostConnection]

  def get(count: Int)(implicit database: Database, ec: ExecutionContext): Seq[PostConnection] = {
    var songs = Seq.empty[PostConnection]

    while (songs.size < count) {
      if (buffer.isEmpty) {
        buffer = Await.result(next(count), 10.seconds)
        if (buffer.isEmpty) {
          return songs
        }
      }
      

      val nextHead = buffer.head // err
      buffer = buffer.tail
      val listen = listens.get(nextHead.song)
      val now = Timestamp.valueOf(LocalDateTime.now())
      listen match {
        case Some(value) =>
          if (now.getTime - value.timestamp.getTime > Radio.TIMESTAMP_EXPIRY.toMillis) {
            val newListen = Listen(now)
            listens.put(nextHead.song, newListen)
            
            val existingViewQuery = UserPostAssociation.table
              .filter(assoc => assoc.subj === viewer && assoc.obj === nextHead.id && assoc.views === true)
              .result.headOption
              
            database.run(existingViewQuery).flatMap {
              case Some(existing) =>
                val updateQuery = UserPostAssociation.table
                  .filter(assoc => assoc.subj === viewer && assoc.obj === nextHead.id)
                  .map(assoc => (assoc.watchCount, assoc.lastInteractionDate))
                  .update((existing.watchCount + 1, Some(Timestamp.valueOf(LocalDateTime.now()))))
                database.run(updateQuery)
              case None =>
                val now = Timestamp.valueOf(LocalDateTime.now())
                val relation = UserPostAssociationConnection(
                  viewer,
                  nextHead.id, 
                  likes = false,
                  views = true,
                  watchCount = 1,
                  lastInteractionDate = Some(now),
                  createdAt = now
                )
                database.run(UserPostAssociation.table += relation)
            }.recover {
              case _: Exception => 0
            }
            
            songs = songs :+ nextHead
          }
        case None => 
          val newListen = Listen(now)
          listens.put(nextHead.song, newListen)
          
          val existingViewQuery = UserPostAssociation.table
            .filter(assoc => assoc.subj === viewer && assoc.obj === nextHead.id && assoc.views === true)
            .result.headOption
            
          database.run(existingViewQuery).flatMap {
            case Some(existing) =>
              val updateQuery = UserPostAssociation.table
                .filter(assoc => assoc.subj === viewer && assoc.obj === nextHead.id)
                .map(assoc => (assoc.watchCount, assoc.lastInteractionDate))
                .update((existing.watchCount + 1, Some(Timestamp.valueOf(LocalDateTime.now()))))
              database.run(updateQuery)
            case None =>
              val now = Timestamp.valueOf(LocalDateTime.now())
              val relation = UserPostAssociationConnection(
                viewer,
                nextHead.id, 
                likes = false,
                views = true,
                watchCount = 1,
                lastInteractionDate = Some(now),
                createdAt = now
              )
              database.run(UserPostAssociation.table += relation)
          }.recover {
            case _: Exception => 0
          }
          
          songs = songs :+ nextHead
      }
    }
    songs
  }

  protected def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]]
}

abstract class RadioType[T <: Radio, A] {
  val radios = mutable.Map[Identity[User], T]()

  protected def create(viewer: Identity[User], arg: A): T

  def apply(viewer: Identity[User], arg: A): T = {
    radios.getOrElseUpdate(viewer, create(viewer, arg))
  }
}

object Radio {
  val TIMESTAMP_EXPIRY = 1.day
}

class UserRadio(override val viewer: Identity[User], val user: Identity[User]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.poster === user).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object UserRadio extends RadioType[UserRadio, Identity[User]] {
  override protected def create(viewer: Identity[User], arg: Identity[User]): UserRadio = new UserRadio(viewer, arg)
}


class CliqueRadio(override val viewer: Identity[User], val clique: Identity[Clique]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.clique === clique).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object CliqueRadio extends RadioType[CliqueRadio, Identity[Clique]] {
  override protected def create(viewer: Identity[User], arg: Identity[Clique]): CliqueRadio = new CliqueRadio(viewer, arg)
}



class ClusterRadio(override val viewer: Identity[User], val cluster: Identity[Cluster]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.cluster === cluster).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object ClusterRadio extends RadioType[ClusterRadio, Identity[Cluster]] {
  override protected def create(viewer: Identity[User], arg: Identity[Cluster]): ClusterRadio = new ClusterRadio(viewer, arg)
}


class GlobalRadio(override val viewer: Identity[User], val none: Unit) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.sortBy(_.rank.desc).take(count).result

    for {
      posts <- database.run(query)
      _ <- Future.sequence(posts.map { post =>
        val updated = post.copy(globalViews = post.globalViews + 1)
        database.run(Post.table.filter(_.id === post.id).update(updated))
      })
    } yield posts
  }
}
object GlobalRadio extends RadioType[GlobalRadio, Unit] {
  override protected def create(viewer: Identity[User], arg: Unit): GlobalRadio = new GlobalRadio(viewer, arg)
}

class CommunityRadio(override val viewer: Identity[User], val community: Identity[Community]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.join(User.table).on(_.poster === _.id).filter(_._2.community === community).sortBy(_._1.rank.desc).take(count).result

    for {
      posts <- database.run(query).map(posts => posts.map(_._1))
      _ <- Future.sequence(posts.map { post =>
        val updated = post.copy(communityViews = post.communityViews + 1)
        database.run(Post.table.filter(_.id === post.id).update(updated))
      })
    } yield posts
    
  }
}
object CommunityRadio extends RadioType[CommunityRadio, Identity[Community]] {
  override protected def create(viewer: Identity[User], arg: Identity[Community]): CommunityRadio = new CommunityRadio(viewer, arg)
}

class FollowingRadio(override val viewer: Identity[User], val none: Unit) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.join(UserUserAssociation.table).on(_.poster === _.obj).filter( x => x._2.subj === viewer && x._2.follows === true).sortBy(_._1.rank.desc).take(count).result
    database.run(query).map(posts => posts.map(_._1))
  }
}
object FollowingRadio extends RadioType[FollowingRadio, Unit] {
  override protected def create(viewer: Identity[User], arg: Unit): FollowingRadio = new FollowingRadio(viewer, arg)
}

class ForYouRadio(override val viewer: Identity[User], val none: Unit) extends Radio(viewer) {
  val random = new Random()
  
  def getCommunity(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    if (count <= 0) return Future.successful(Seq.empty)
    
    val userCommunityQuery = User.table.filter(_.id === viewer).map(_.community).result.head
    val communityPostsQuery = for {
      userCommunity <- userCommunityQuery
      posts <- Post.table
        .join(User.table).on(_.poster === _.id)
        .filter(_._2.community === userCommunity)
        .sortBy(_._1.rank.desc)
        .take(count)
        .result
    } yield posts.map(_._1)
    
    database.run(communityPostsQuery).recover(_ => Seq.empty)
  }

  def getFollowing(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    if (count <= 0) return Future.successful(Seq.empty)
    
    val followingUsersQuery = UserUserAssociation.table
      .filter(assoc => assoc.subj === viewer && assoc.follows === true)
      .map(_.obj)
      .result
    
    val followingPostsQuery = for {
      followingUsers <- followingUsersQuery
      posts <- Post.table
        .filter(_.poster.inSet(followingUsers))
        .sortBy(_.rank.desc)
        .take(count)
        .result
    } yield posts
    
    database.run(followingPostsQuery).recover(_ => Seq.empty)
  }
  
  def getGlobal(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    if (count <= 0) return Future.successful(Seq.empty)
    
    val globalPostsQuery = Post.table
      .sortBy(_.rank.desc)
      .take(count)
      .result
    
    database.run(globalPostsQuery).recover(_ => Seq.empty)
  }

  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    var communityCount = 0
    var followingCount = 0
    var globalCount = 0

    for (_ <- 0 until count) {
      val randomNumber = random.nextDouble()
      if (randomNumber < 0.4) {
        communityCount += 1
      } else if (randomNumber < 0.9) {
        followingCount += 1
      } else {
        globalCount += 1
      }
    }

    for {
      community <- getCommunity(communityCount)
      following <- getFollowing(followingCount)
      global <- getGlobal(globalCount)
    } yield {
      val combined = community ++ following ++ global
      random.shuffle(combined).take(count)
    }
  }
}

object ForYouRadio extends RadioType[ForYouRadio, Unit] {
  override protected def create(viewer: Identity[User], arg: Unit): ForYouRadio = new ForYouRadio(viewer, arg)
}