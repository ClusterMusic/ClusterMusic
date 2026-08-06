package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class CliqueAchievement

object CliqueAchievement {
  lazy val table = TableQuery[CliqueAchievementTable]
}

case class CliqueAchievementConnection(
                                     id: Identity[CliqueAchievement],
                                     name: String,
                                     createdAt: Timestamp
                                   )

class CliqueAchievementTable(tag: Tag) extends Table[CliqueAchievementConnection](tag, "clique_achievement") {
  def id = column[Identity[CliqueAchievement]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def createdAt = column[Timestamp]("created_at")

  override def * : ProvenShape[CliqueAchievementConnection] = (id, name, createdAt).mapTo[CliqueAchievementConnection]
} 