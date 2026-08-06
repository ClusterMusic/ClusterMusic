package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class CliqueAchievementAssociation

object CliqueAchievementAssociation {
  lazy val table = TableQuery[CliqueAchievementAssociationTable]
}

case class CliqueAchievementAssociationConnection(
                                                subj: Identity[Clique],
                                                obj: Identity[CliqueAchievement],
                                                createdAt: Timestamp
                                              )

class CliqueAchievementAssociationTable(tag: Tag) extends Table[CliqueAchievementAssociationConnection](tag, "clique_achievement_association") {
  def subj = column[Identity[Clique]]("subj")
  def obj = column[Identity[CliqueAchievement]]("obj")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_clique_achievement", (subj, obj))
  
  def idxSubj = index("idx_clique_achievement_subj", subj)
  def idxObj = index("idx_clique_achievement_obj", obj)

  override def * : ProvenShape[CliqueAchievementAssociationConnection] = (subj, obj, createdAt).mapTo[CliqueAchievementAssociationConnection]
} 