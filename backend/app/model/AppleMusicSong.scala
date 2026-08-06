package model

import slick.ast.BaseTypedType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

class AppleMusicSong

object AppleMusicSong {
  lazy val table = TableQuery[AppleMusicSongTable]
}

case class AppleMusicSongConnection(
                                  id: Identity[AppleMusicSong],
                                  foundAt: Timestamp
                                )

class AppleMusicSongTable(tag: Tag) extends Table[AppleMusicSongConnection](tag, "apple_music_song") {

  def id = column[Identity[AppleMusicSong]]("id", O.PrimaryKey, O.Unique)
  def foundAt = column[Timestamp]("found_at")

  override def * : ProvenShape[AppleMusicSongConnection] = (id, foundAt).mapTo[AppleMusicSongConnection]
} 