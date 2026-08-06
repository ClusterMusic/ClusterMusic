package model

import java.sql.Timestamp
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserUserAssociation

object UserUserAssociation {
  lazy val table = TableQuery[UserUserAssociationTable]
}

case class UserUserAssociationConnection(
                                       subj: Identity[User],
                                       obj: Identity[User],
                                       follows: Boolean,
                                       lastInteractionDate: Option[Timestamp],
                                       createdAt: Timestamp
                                     )

class UserUserAssociationTable(tag: Tag) extends Table[UserUserAssociationConnection](tag, "user_user_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[User]]("obj")
  def follows = column[Boolean]("follows")
  def lastInteractionDate = column[Option[Timestamp]]("last_interaction_date")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_user_user", (subj, obj))
  
  def idxSubj = index("idx_user_user_subj", subj)
  def idxObj = index("idx_user_user_obj", obj)
  def idxFollows = index("idx_user_user_follows", follows)

  override def * : ProvenShape[UserUserAssociationConnection] = (subj, obj, follows, lastInteractionDate, createdAt).mapTo[UserUserAssociationConnection]
} 