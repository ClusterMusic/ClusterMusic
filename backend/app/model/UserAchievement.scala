package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserAchievement

object UserAchievement {
  lazy val table = TableQuery[UserAchievementTable]
}

case class UserAchievementConnection(
                                   id: Identity[UserAchievement],
                                   name: String,
                                   createdAt: Timestamp
                                 )

class UserAchievementTable(tag: Tag) extends Table[UserAchievementConnection](tag, "user_achievement") {
  def id = column[Identity[UserAchievement]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def createdAt = column[Timestamp]("created_at")

  override def * : ProvenShape[UserAchievementConnection] = (id, name, createdAt).mapTo[UserAchievementConnection]
} 