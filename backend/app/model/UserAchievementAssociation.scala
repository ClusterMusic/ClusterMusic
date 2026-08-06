package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserAchievementAssociation

object UserAchievementAssociation {
  lazy val table = TableQuery[UserAchievementAssociationTable]
}

case class UserAchievementAssociationConnection(
                                              subj: Identity[User],
                                              obj: Identity[UserAchievement],
                                              createdAt: Timestamp
                                            )

class UserAchievementAssociationTable(tag: Tag) extends Table[UserAchievementAssociationConnection](tag, "user_achievement_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[UserAchievement]]("obj")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_user_achievement", (subj, obj))
  def subjFK = foreignKey("fk_user_achievement_subj", subj, User.table)(_.id)
  def objFK = foreignKey("fk_user_achievement_obj", obj, UserAchievement.table)(_.id)
  
  def idxSubj = index("idx_user_achievement_subj", subj)
  def idxObj = index("idx_user_achievement_obj", obj)

  override def * : ProvenShape[UserAchievementAssociationConnection] = (subj, obj, createdAt).mapTo[UserAchievementAssociationConnection]
} 