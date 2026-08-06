package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserSettingsEntity

object UserSettings {
  val table = TableQuery[UserSettingsTable]
}

// Privacy settings
case class PrivacySettings(
                            profileVisible: Boolean = false,
                            showActivityStatus: Boolean = true,
                            allowTagging: Boolean = true
                          )

// Notification settings
case class NotificationSettings(
                                 push: Boolean = true,
                                 mentions: Boolean = true,
                                 messages: Boolean = true,
                                 newFollowers: Boolean = true
                               )

// UI settings
case class UISettings(
                       language: String = "en",
                       fontSize: Int = 16
                     )

// Player settings
case class PlayerSettings(
                           defaultService: String = "spotify"
                         )

// Location data
case class LocationData(
                         latitude: Option[Double] = None,
                         longitude: Option[Double] = None,
                         city: Option[String] = None,
                         region: Option[String] = None
                       )

// Full settings blob for API response
case class UserSettingsBlob(
                             userId: Int,
                             privacy: PrivacySettings,
                             notifications: NotificationSettings,
                             ui: UISettings,
                             player: PlayerSettings,
                             location: Option[LocationData]
                           )

// Database connection model - stores settings as JSON strings
case class UserSettingsConnection(
                                   id: Identity[UserSettingsEntity],
                                   userId: Identity[User],
                                   privacyJson: String,
                                   notificationsJson: String,
                                   uiJson: String,
                                   playerJson: String,
                                   locationJson: Option[String],
                                   updatedAt: Timestamp
                                 )

class UserSettingsTable(tag: Tag) extends Table[UserSettingsConnection](tag, "user_settings") {
  def id = column[Identity[UserSettingsEntity]]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Identity[User]]("user_id", O.Unique)
  def privacyJson = column[String]("privacy_json")
  def notificationsJson = column[String]("notifications_json")
  def uiJson = column[String]("ui_json")
  def playerJson = column[String]("player_json")
  def locationJson = column[Option[String]]("location_json")
  def updatedAt = column[Timestamp]("updated_at")

  def idxUserId = index("idx_user_settings_user_id", userId, unique = true)

  override def * : ProvenShape[UserSettingsConnection] = 
    (id, userId, privacyJson, notificationsJson, uiJson, playerJson, locationJson, updatedAt).mapTo[UserSettingsConnection]
}
