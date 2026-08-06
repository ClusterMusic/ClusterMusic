package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class Community

object Community {
  lazy val table = TableQuery[CommunityTable]
}

case class CommunityBlob(
                        id: Identity[Community],
                        image: String,
                        title: String,
                        location: String,
                        badges: Seq[String]
                      )

case class CommunityConnection(
                             id: Identity[Community],
                             image: String,
                             title: String,
                             area_geojson: String,
                             createdAt: Timestamp
                           ) {
  def blob: CommunityBlob = {
    CommunityBlob(id, image, title, Temp.regionName, Temp.communityBadges)
  }
}

class CommunityTable(tag: Tag) extends Table[CommunityConnection](tag, "community") {
  def id = column[Identity[Community]]("id", O.PrimaryKey, O.AutoInc)
  def image = column[String]("image")
  def title = column[String]("title")
  def area_geojson = column[String]("area_geojson")
  def createdAt = column[Timestamp]("created_at")

  override def * : ProvenShape[CommunityConnection] = (id, image, title, area_geojson, createdAt).mapTo[CommunityConnection]
} 