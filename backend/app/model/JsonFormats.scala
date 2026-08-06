package model

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}

import java.sql.Timestamp

object JsonFormats {
  implicit val timestampFormat: Format[Timestamp] = Format(
    Reads.of[String].map(s => Timestamp.valueOf(s)),
    Writes.of[String].contramap[Timestamp](_.toString)
  )

  implicit val communityConnectionFormat: OFormat[CommunityConnection] = Json.format[CommunityConnection]
  implicit val communityBlobFormat: OFormat[CommunityBlob] = Json.format[CommunityBlob]

  implicit val clusterConnectionFormat: OFormat[ClusterConnection] = Json.format[ClusterConnection]
  implicit val clusterBlobFormat: OFormat[ClusterBlob] = Json.format[ClusterBlob]
  implicit val clusterProfileFormat: OFormat[ClusterProfile] = Json.format[ClusterProfile]

  implicit val userConnectionFormat: OFormat[UserConnection] = Json.format[UserConnection]
  implicit val userBlobFormat: OFormat[UserBlob] = Json.format[UserBlob]
  implicit val userProfileFormat: OFormat[UserProfile] = Json.format[UserProfile]

  implicit val postConnectionFormat: OFormat[PostConnection] = Json.format[PostConnection]
  implicit val postBlobFormat: OFormat[PostBlob] = Json.format[PostBlob]
  implicit val commentConnectionFormat: OFormat[CommentConnection] = Json.format[CommentConnection]

  implicit val cliqueConnectionFormat: OFormat[CliqueConnection] = Json.format[CliqueConnection]
  implicit val cliqueBlobFormat: OFormat[CliqueBlob] = Json.format[CliqueBlob]
  implicit val cliqueProfileFormat: OFormat[CliqueProfile] = Json.format[CliqueProfile]


  implicit val songConnectionFormat: OFormat[SongConnection] = Json.format[SongConnection]
  implicit val songBlobFormat: OFormat[SongBlob] = Json.format[SongBlob]
  implicit val userAchievementConnectionFormat: OFormat[UserAchievementConnection] = Json.format[UserAchievementConnection]
  implicit val cliqueAchievementConnectionFormat: OFormat[CliqueAchievementConnection] = Json.format[CliqueAchievementConnection]
  implicit val userUserAssociationConnectionFormat: OFormat[UserUserAssociationConnection] = Json.format[UserUserAssociationConnection]
  implicit val userClusterAssociationConnectionFormat: OFormat[UserClusterAssociationConnection] = Json.format[UserClusterAssociationConnection]
  implicit val userCliqueAssociationConnectionFormat: OFormat[UserCliqueAssociationConnection] = Json.format[UserCliqueAssociationConnection]
  implicit val userCommentAssociationConnectionFormat: OFormat[UserCommentAssociationConnection] = Json.format[UserCommentAssociationConnection]
  implicit val userPostAssociationConnectionFormat: OFormat[UserPostAssociationConnection] = Json.format[UserPostAssociationConnection]
  implicit val userAchievementAssociationConnectionFormat: OFormat[UserAchievementAssociationConnection] = Json.format[UserAchievementAssociationConnection]
  implicit val cliqueAchievementAssociationConnectionFormat: OFormat[CliqueAchievementAssociationConnection] = Json.format[CliqueAchievementAssociationConnection]

  // User Settings formats
  implicit val privacySettingsFormat: OFormat[PrivacySettings] = Json.format[PrivacySettings]
  implicit val notificationSettingsFormat: OFormat[NotificationSettings] = Json.format[NotificationSettings]
  implicit val uiSettingsFormat: OFormat[UISettings] = Json.format[UISettings]
  implicit val playerSettingsFormat: OFormat[PlayerSettings] = Json.format[PlayerSettings]
  implicit val locationDataFormat: OFormat[LocationData] = Json.format[LocationData]
  implicit val userSettingsBlobFormat: OFormat[UserSettingsBlob] = Json.format[UserSettingsBlob]
  implicit val userSettingsConnectionFormat: OFormat[UserSettingsConnection] = Json.format[UserSettingsConnection]

}
