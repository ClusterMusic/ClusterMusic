package model

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

import java.sql.Timestamp

object JsonFormats {
  implicit lazy val timestampFormat: Format[Timestamp] = Format(
    Reads.of[String].map(s => Timestamp.valueOf(s)),
    Writes.of[String].contramap[Timestamp](_.toString)
  )

  implicit lazy val communityConnectionFormat: OFormat[CommunityConnection] = Json.format[CommunityConnection]
  implicit lazy val communityBlobFormat: OFormat[CommunityBlob] = Json.format[CommunityBlob]

  implicit lazy val clusterConnectionFormat: OFormat[ClusterConnection] = Json.format[ClusterConnection]
  implicit lazy val clusterBlobFormat: OFormat[ClusterBlob] = Json.format[ClusterBlob]
  implicit lazy val clusterProfileFormat: OFormat[ClusterProfile] = Json.format[ClusterProfile]

  implicit lazy val userConnectionFormat: OFormat[UserConnection] = Json.format[UserConnection]
  implicit lazy val userBlobFormat: OFormat[UserBlob] = Json.format[UserBlob]
  implicit lazy val userProfileFormat: OFormat[UserProfile] = Json.format[UserProfile]

  implicit lazy val postConnectionFormat: OFormat[PostConnection] = Json.format[PostConnection]
  implicit lazy val postBlobFormat: OFormat[PostBlob] = Json.format[PostBlob]
  implicit lazy val commentConnectionFormat: OFormat[CommentConnection] = Json.format[CommentConnection]

  implicit lazy val cliqueConnectionFormat: OFormat[CliqueConnection] = Json.format[CliqueConnection]
  implicit lazy val cliqueBlobFormat: OFormat[CliqueBlob] = Json.format[CliqueBlob]
  implicit lazy val cliqueProfileFormat: OFormat[CliqueProfile] = Json.format[CliqueProfile]


  implicit lazy val songConnectionFormat: OFormat[SongConnection] = Json.format[SongConnection]
  implicit lazy val songBlobFormat: OFormat[SongBlob] = Json.format[SongBlob]
  implicit lazy val userAchievementConnectionFormat: OFormat[UserAchievementConnection] = Json.format[UserAchievementConnection]
  implicit lazy val cliqueAchievementConnectionFormat: OFormat[CliqueAchievementConnection] = Json.format[CliqueAchievementConnection]
  implicit lazy val userUserAssociationConnectionFormat: OFormat[UserUserAssociationConnection] = Json.format[UserUserAssociationConnection]
  implicit lazy val userClusterAssociationConnectionFormat: OFormat[UserClusterAssociationConnection] = Json.format[UserClusterAssociationConnection]
  implicit lazy val userCliqueAssociationConnectionFormat: OFormat[UserCliqueAssociationConnection] = Json.format[UserCliqueAssociationConnection]
  implicit lazy val userCommentAssociationConnectionFormat: OFormat[UserCommentAssociationConnection] = Json.format[UserCommentAssociationConnection]
  implicit lazy val userPostAssociationConnectionFormat: OFormat[UserPostAssociationConnection] = Json.format[UserPostAssociationConnection]
  implicit lazy val userAchievementAssociationConnectionFormat: OFormat[UserAchievementAssociationConnection] = Json.format[UserAchievementAssociationConnection]
  implicit lazy val cliqueAchievementAssociationConnectionFormat: OFormat[CliqueAchievementAssociationConnection] = Json.format[CliqueAchievementAssociationConnection]

  // User Settings formats
  implicit lazy val privacySettingsFormat: OFormat[PrivacySettings] = Json.format[PrivacySettings]
  implicit lazy val notificationSettingsFormat: OFormat[NotificationSettings] = Json.format[NotificationSettings]
  implicit lazy val uiSettingsFormat: OFormat[UISettings] = Json.format[UISettings]
  implicit lazy val playerSettingsFormat: OFormat[PlayerSettings] = Json.format[PlayerSettings]
  implicit lazy val locationDataFormat: OFormat[LocationData] = Json.format[LocationData]
  implicit lazy val userSettingsBlobFormat: OFormat[UserSettingsBlob] = Json.format[UserSettingsBlob]
  implicit lazy val userSettingsConnectionFormat: OFormat[UserSettingsConnection] = Json.format[UserSettingsConnection]

}
