package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class SpotifySong

object SpotifySong {
  lazy val table = TableQuery[SpotifySongTable]
}

case class SpotifySongConnection(
                               id: Identity[SpotifySong],
                               foundAt: Timestamp
                             )

class SpotifySongTable(tag: Tag) extends Table[SpotifySongConnection](tag, "spotify_song") {
  def id = column[Identity[SpotifySong]]("id", O.PrimaryKey, O.AutoInc)
  def foundAt = column[Timestamp]("found_at")

  override def * : ProvenShape[SpotifySongConnection] = (id, foundAt).mapTo[SpotifySongConnection]
} 