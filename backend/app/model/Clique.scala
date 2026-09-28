package model

import chat.ChatService
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.{Format, JsNumber, Reads, Writes}

import java.sql.Timestamp

class CliqueMemberRole(val id: Int){
  override def equals(obj: Any): Boolean = obj match {
    case that: CliqueMemberRole => this.id == that.id
    case _ => false
  }


}
object CliqueMemberRole {
  def apply(id: Int) = new CliqueMemberRole(id)
  implicit def memberRoleMapping: JdbcType[CliqueMemberRole] with BaseTypedType[CliqueMemberRole] = MappedColumnType.base[CliqueMemberRole, Int](
    role => role.id,
    id => CliqueMemberRole(id)
  )

  implicit val cliqueMemberRoleFormat: Format[CliqueMemberRole] = Format(
    Reads.IntReads.map(CliqueMemberRole(_)),
    Writes(role => JsNumber(role.id))
  )

  val NoRole = new CliqueMemberRole(0)
  val Following = new CliqueMemberRole(1)
  val Member = new CliqueMemberRole(2)
  val Moderator = new CliqueMemberRole(3)
  val Admin = new CliqueMemberRole(4)
}

class Clique

object Clique {
  lazy val table = TableQuery[CliqueTable]
}

case class CliqueBlob(
                     id: Identity[Clique],
                     image: String,
                     name: String,
                     biography: String,
                     tags: Seq[String],
                     score: Double,
                     rank: Double
                     )

case class CliqueProfile(
                        id: Identity[Clique],
                        image: String,
                        name: String,
                        biography: String,
                        tags: Seq[String],
                        members: Int,
                        followers: Int,
                        score: Double,
                        rank: Double
                        )

case class CliqueConnection(
                           id: Identity[Clique],
                           name: String,
                           biography: String,
                           chatroom: Int,
                           createdAt: Timestamp,
                           score: Double,
                           rank: Double
                         ) {

  def toBlob: CliqueBlob =
    CliqueBlob(id, Temp.imageURL, name, biography, Temp.cliqueTags, score, rank)

  def blob(implicit database: Database, ec: ExecutionContext): Future[CliqueBlob] =
    Future.successful(toBlob)

  def profile(implicit database: Database, ec: ExecutionContext): Future[CliqueProfile] = {
    val membersQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.obj === id && (assoc.memberRole === CliqueMemberRole.Member || 
                          assoc.memberRole === CliqueMemberRole.Moderator || 
                          assoc.memberRole === CliqueMemberRole.Admin)
    ).length.result
    val followersQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.memberRole === CliqueMemberRole.Following
    ).length.result
    for {
      members <- database.run(membersQuery).recover(_ => 0)
      followers <- database.run(followersQuery). recover(_ => 0)
    } yield CliqueProfile(id, Temp.imageURL, name, biography, Temp.cliqueTags, members, followers, score, rank)
  }

  def updateCliqueScore()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
    val postScoresQuery = Post.table.filter(_.clique === id).map(_.score).result
    val followersQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.memberRole === CliqueMemberRole.Following
    ).length.result
    val membersQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.obj === id && (assoc.memberRole === CliqueMemberRole.Member || 
                          assoc.memberRole === CliqueMemberRole.Moderator || 
                          assoc.memberRole === CliqueMemberRole.Admin)
    ).length.result
    val activeMembersQuery = UserCliqueAssociation.table.filter(assoc =>
      assoc.obj === id && (assoc.memberRole === CliqueMemberRole.Member || 
                          assoc.memberRole === CliqueMemberRole.Moderator || 
                          assoc.memberRole === CliqueMemberRole.Admin) &&
      assoc.joinDate.isDefined
    ).length.result

    for {
      postScores <- database.run(postScoresQuery)
      followers <- database.run(followersQuery)
      members <- database.run(membersQuery)
      activeMembers <- database.run(activeMembersQuery)
      newScore = if (postScores.nonEmpty) {
        Scoring.cliqueScore(postScores, followers, members, activeMembers)
      } else 0.0
      out <- database.run(sqlu"UPDATE clique SET score = $newScore WHERE id = ${id.value}")
    } yield {
      out > 0
    }
  }

  def updateCliqueRank()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
    val offset = java.time.ZoneOffset.UTC
    val timeDays = (LocalDateTime.now().toEpochSecond(offset) - createdAt.toInstant.getEpochSecond) / (24 * 60 * 60)
    val newRank = Scoring.cliqueRank(score, timeDays)
    database.run(sqlu"UPDATE clique SET rank = $newRank WHERE id = ${id.value}").map(_ > 0)
  }
}

class CliqueTable(tag: Tag) extends Table[CliqueConnection](tag, "clique") {
  def id = column[Identity[Clique]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.Length(128, varying = true), O.Unique)
  def biography = column[String]("biography")
  def chatroom = column[Int]("chatroom")
  def createdAt = column[Timestamp]("created_at")
  def score = column[Double]("score")
  def rank = column[Double]("rank")

  def idxCreatedAt = index("idx_clique_created_at", createdAt)

  override def * : ProvenShape[CliqueConnection] = (id, name, biography, chatroom, createdAt, score, rank).mapTo[CliqueConnection]
} 