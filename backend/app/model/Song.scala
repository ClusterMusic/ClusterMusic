package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class Song

object Song {
    lazy val table = TableQuery[SongTable]
}

case class SongBlob(
    id: Identity[Song],
    title: String,
    artist: String,
    album: String,
    duration: String,
    spotifyId: Option[String],
    appleMusicId: Option[String]
)

case class SongConnection(
    id: Identity[Song],
    title: String,
    spotify_id: Option[Identity[SpotifySong]],
    apple_music_id: Option[Identity[AppleMusicSong]],
    createdAt: Timestamp,
    score: Int
) {

  def toBlob: SongBlob = SongBlob(
    id = id,
    title = title,
    artist = "Unknown Artist",
    album = "Unknown Album",
    duration = "3:30",
    spotifyId = spotify_id.map(_.value.toString),
    appleMusicId = apple_music_id.map(_.value.toString)
  )

  def blob(implicit database: Database, ec: ExecutionContext): Future[SongBlob] =
    Future.successful(toBlob)
}

class SongTable(tag: Tag) extends Table[SongConnection](tag, "song") {
    def id = column[Identity[Song]]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def spotify_id = column[Option[Identity[SpotifySong]]]("spotify_id")
    def apple_music_id = column[Option[Identity[AppleMusicSong]]]("apple_music_id")
    def createdAt = column[Timestamp]("created_at")
    def score = column[Int]("score")

    override def * : ProvenShape[SongConnection] = (id, title, spotify_id, apple_music_id, createdAt, score).mapTo[SongConnection]
}
