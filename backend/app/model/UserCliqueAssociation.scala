package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

class UserCliqueAssociation

object UserCliqueAssociation {
  lazy val table = TableQuery[UserCliqueAssociationTable]
}

case class UserCliqueAssociationConnection(
                                         subj: Identity[User],
                                         obj: Identity[Clique],
                                         memberRole: CliqueMemberRole,
                                         joinDate: Option[Timestamp],
                                         lastInteractionDate: Option[Timestamp],
                                         createdAt: Timestamp
                                       )

class UserCliqueAssociationTable(tag: Tag) extends Table[UserCliqueAssociationConnection](tag, "user_clique_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[Clique]]("obj")
  def memberRole = column[CliqueMemberRole]("member_role")
  def joinDate = column[Option[Timestamp]]("join_date")
  def lastInteractionDate = column[Option[Timestamp]]("last_interaction_date")
  def createdAt = column[Timestamp]("created_at", O.SqlType("DATETIME"))

  def pk = primaryKey("pk_user_clique", (subj, obj))
  def subjFK = foreignKey("fk_user_clique_subj", subj, User.table)(_.id)
  def objFK = foreignKey("fk_user_clique_obj", obj, Clique.table)(_.id)
  
  def idxSubj = index("idx_user_clique_subj", subj)
  def idxObj = index("idx_user_clique_obj", obj)
  def idxMemberRole = index("idx_user_clique_member_role", memberRole)

  override def * : ProvenShape[UserCliqueAssociationConnection] = (subj, obj, memberRole, joinDate, lastInteractionDate, createdAt).mapTo[UserCliqueAssociationConnection]
} 