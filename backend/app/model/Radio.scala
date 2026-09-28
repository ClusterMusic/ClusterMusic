package model

import slick.jdbc.MySQLProfile.api._

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random


abstract class Radio(val viewer: Identity[User]) {

  def get(requestedCount: Int)(implicit database: Database, ec: ExecutionContext): Seq[PostConnection] = {
    if (requestedCount <= 0) return Seq.empty

    val count = math.min(requestedCount, Radio.MAX_PAGE)

    val candidates = Await.result(next(count * Radio.OVER_FETCH), 10.seconds)

    val selected = candidates.distinctBy(_.song).take(count)

    recordViews(selected)
    selected
  }

  protected def heardSongIds: Query[Rep[Identity[Song]], Identity[Song], Seq] = {
    val cutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Radio.TIMESTAMP_EXPIRY.toSeconds))

    UserPostAssociation.table
      .filter(assoc => assoc.subj === viewer && assoc.views === true)
      .join(Post.table).on(_.obj === _.id)
      .filter { case (assoc, _) => assoc.lastInteractionDate.getOrElse(Radio.NeverPlayed) > cutoff }
      .map { case (_, post) => post.song }
  }

  private def recordViews(posts: Seq[PostConnection])(implicit database: Database, ec: ExecutionContext): Unit = {
    if (posts.isEmpty) return

    val now = Timestamp.valueOf(LocalDateTime.now())
    val valuesClause = List.fill(posts.size)("(?, ?, 0, 1, 1, ?, ?)").mkString(", ")
    val sql =
      s"""INSERT INTO user_post_association
            (subj, obj, likes, views, watch_count, last_interaction_date, created_at)
          VALUES $valuesClause
          ON DUPLICATE KEY UPDATE
            views = 1,
            watch_count = watch_count + 1,
            last_interaction_date = VALUES(last_interaction_date)"""

    val action = SimpleDBIO { context =>
      val statement = context.session.conn.prepareStatement(sql)
      try {
        posts.zipWithIndex.foreach { case (post, index) =>
          val base = index * 4
          statement.setInt(base + 1, viewer.value)
          statement.setInt(base + 2, post.id.value)
          statement.setTimestamp(base + 3, now)
          statement.setTimestamp(base + 4, now)
        }
        statement.executeUpdate()
      } finally {
        statement.close()
      }
    }

    database.run(action).recover { case _: Exception => 0 }
  }

  protected def incrementViewCounter(
    column: String,
    ids: Seq[Identity[Post]]
  )(implicit database: Database, ec: ExecutionContext): Future[Int] = {
    if (ids.isEmpty) return Future.successful(0)

    val placeholders = List.fill(ids.size)("?").mkString(", ")
    val sql = s"UPDATE post SET $column = $column + 1 WHERE id IN ($placeholders)"

    val action = SimpleDBIO { context =>
      val statement = context.session.conn.prepareStatement(sql)
      try {
        ids.zipWithIndex.foreach { case (id, index) => statement.setInt(index + 1, id.value) }
        statement.executeUpdate()
      } finally {
        statement.close()
      }
    }

    database.run(action).recover { case _: Exception => 0 }
  }

  protected def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]]
}

abstract class RadioType[T <: Radio, A] {

  protected def create(viewer: Identity[User], arg: A): T


  def apply(viewer: Identity[User], arg: A): T = create(viewer, arg)
}

object Radio {
  val TIMESTAMP_EXPIRY = 1.day

  val MAX_PAGE = 100

  val OVER_FETCH = 1

  val NeverPlayed: Timestamp = new Timestamp(0L)
}

class UserRadio(override val viewer: Identity[User], val user: Identity[User]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.poster === user).filterNot(_.song in heardSongIds).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object UserRadio extends RadioType[UserRadio, Identity[User]] {
  override protected def create(viewer: Identity[User], arg: Identity[User]): UserRadio = new UserRadio(viewer, arg)
}


class CliqueRadio(override val viewer: Identity[User], val clique: Identity[Clique]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.clique === clique).filterNot(_.song in heardSongIds).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object CliqueRadio extends RadioType[CliqueRadio, Identity[Clique]] {
  override protected def create(viewer: Identity[User], arg: Identity[Clique]): CliqueRadio = new CliqueRadio(viewer, arg)
}



class ClusterRadio(override val viewer: Identity[User], val cluster: Identity[Cluster]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.cluster === cluster).filterNot(_.song in heardSongIds).sortBy(_.rank.desc).take(count).result
    database.run(query)
  }
}

object ClusterRadio extends RadioType[ClusterRadio, Identity[Cluster]] {
  override protected def create(viewer: Identity[User], arg: Identity[Cluster]): ClusterRadio = new ClusterRadio(viewer, arg)
}


class GlobalRadio(override val viewer: Identity[User], val none: Unit) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filterNot(_.song in heardSongIds).sortBy(_.rank.desc).take(count).result

    for {
      posts <- database.run(query)
      _ <- incrementViewCounter("global_views", posts.map(_.id))
    } yield posts
  }
}
object GlobalRadio extends RadioType[GlobalRadio, Unit] {
  override protected def create(viewer: Identity[User], arg: Unit): GlobalRadio = new GlobalRadio(viewer, arg)
}

class CommunityRadio(override val viewer: Identity[User], val community: Identity[Community]) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filter(_.community === community).filterNot(_.song in heardSongIds).sortBy(_.rank.desc).take(count).result

    for {
      posts <- database.run(query)
      _ <- incrementViewCounter("community_views", posts.map(_.id))
    } yield posts
    
  }
}
object CommunityRadio extends RadioType[CommunityRadio, Identity[Community]] {
  override protected def create(viewer: Identity[User], arg: Identity[Community]): CommunityRadio = new CommunityRadio(viewer, arg)
}

class FollowingRadio(override val viewer: Identity[User], val none: Unit) extends Radio(viewer) {
  override def next(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    val query = Post.table.filterNot(_.song in heardSongIds).join(UserUserAssociation.table).on(_.poster === _.obj).filter( x => x._2.subj === viewer && x._2.follows === true).sortBy(_._1.rank.desc).take(count).result
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
        .filter(_.community === userCommunity)
        .filterNot(_.song in heardSongIds)
        .sortBy(_.rank.desc)
        .take(count)
        .result
    } yield posts
    
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
        .filterNot(_.song in heardSongIds)
        .sortBy(_.rank.desc)
        .take(count)
        .result
    } yield posts
    
    database.run(followingPostsQuery).recover(_ => Seq.empty)
  }
  
  def getGlobal(count: Int)(implicit database: Database, ec: ExecutionContext): Future[Seq[PostConnection]] = {
    if (count <= 0) return Future.successful(Seq.empty)
    
    val globalPostsQuery = Post.table
      .filterNot(_.song in heardSongIds)
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