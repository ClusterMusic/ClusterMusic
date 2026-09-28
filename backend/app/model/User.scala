package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class User

object User {
  val table = TableQuery[UserTable]
}

case class UserBlob(
                   id: Identity[User],
                   image: String,
                   username: String,
                   biography: String,
                   community: CommunityBlob,
                   rank: Double,
                   score: Double
                   )

case class UserProfile(
                      id: Identity[User],
                      image: String,
                      username: String,
                      biography: String,
                      community: CommunityBlob,
                      followers: Int,
                      following: Int,
                      rank: Double,
                      score: Double
                      )

case class UserConnection(
                         id: Identity[User],
                         username: String,
                         hashedKey: String,
                         name: String,
                         image: String,
                         biography: String,
                         community: Identity[Community],
                         createdAt: Timestamp,
                         rank: Double,
                         score: Double
                         ) {
  def toBlob(communityBlob: CommunityBlob): UserBlob =
    UserBlob(id, Temp.imageURL, username, biography, communityBlob, rank, score)

  def blob(implicit database: Database, ec: ExecutionContext): Future[UserBlob] = {
    val communityQuery = Community.table.filter(_.id === community).result.head
    database.run(communityQuery)
      .map(communityConn => toBlob(communityConn.blob))
      .recover(_ => toBlob(Blobs.UnknownCommunity))
  }

  def profile(implicit database: Database, ec: ExecutionContext): Future[UserProfile] = {
    val communityQuery = Community.table.filter(_.id === community).result.head
    val followersQuery = UserUserAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.follows === true
    ).length.result
    val followingQuery = UserUserAssociation.table.filter(assoc =>
      assoc.subj === id && assoc.follows === true
    ).length.result
    for {
      communityblob <- database.run(communityQuery).map(_.blob).recover(_ => CommunityBlob(Identity(-1), "Unknown", "unknown", "Unknown", Seq()))
      followers <- database.run(followersQuery).recover(_ => -1)
      following <- database.run(followingQuery).recover(_ => -1)
    } yield UserProfile(id, Temp.imageURL, username, biography, communityblob, followers, following, rank, score)
  }

  def updateUserRank()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
    val postScoresQuery = Post.table.filter(_.poster === id).map(_.score).result
    val followersQuery = UserUserAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.follows === true
    ).length.result
    val followingQuery = UserUserAssociation.table.filter(assoc =>
      assoc.subj === id && assoc.follows === true
    ).length.result

    for {
      postScores <- database.run(postScoresQuery)
      followers <- database.run(followersQuery)
      following <- database.run(followingQuery)
      offset = java.time.ZoneOffset.UTC
      timeDays = (LocalDateTime.now().toEpochSecond(offset) - createdAt.toInstant.getEpochSecond) / (24 * 60 * 60)
      newRank = Scoring.userRank(postScores, followers, following, timeDays)
      out <- database.run(sqlu"UPDATE user SET rank = $newRank WHERE id = ${id.value}")
    } yield {
      out > 0
    }
  }

  def updateUserScore()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
    val postScoresQuery = Post.table.filter(_.poster === id).map(_.score).result
    val followersQuery = UserUserAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.follows === true
    ).length.result
    val followingQuery = UserUserAssociation.table.filter(assoc =>
      assoc.subj === id && assoc.follows === true
    ).length.result
    val cliqueCountQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.subj === id && (assoc.memberRole === CliqueMemberRole.Member || 
                           assoc.memberRole === CliqueMemberRole.Moderator || 
                           assoc.memberRole === CliqueMemberRole.Admin)
    ).length.result

    for {
      postScores <- database.run(postScoresQuery)
      followers <- database.run(followersQuery)
      following <- database.run(followingQuery)
      cliqueCount <- database.run(cliqueCountQuery)
      newScore = Scoring.userScore(postScores, followers, following, cliqueCount)
      out <- database.run(sqlu"UPDATE user SET score = $newScore WHERE id = ${id.value}")
    } yield {
      out > 0
    }
  }
}

class UserTable(tag: Tag) extends Table[UserConnection](tag, "user"){
  def id = column[Identity[User]]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.Length(32, varying = true), O.Unique)
  def hashedKey = column[String]("hashedKey")
  def name = column[String]("name")
  def image = column[String]("image")
  def biography = column[String]("biography")
  def community = column[Identity[Community]]("community_id")
  def createdAt = column[Timestamp]("created_at")
  def rank = column[Double]("rank")
  def score = column[Double]("score")
  def idxCommunity = index("idx_user_community", community)
  def idxCreatedAt = index("idx_user_created_at", createdAt)

  override def * : ProvenShape[UserConnection] = (id, username, hashedKey, name, image, biography, community, createdAt, rank, score).mapTo[UserConnection]
}

