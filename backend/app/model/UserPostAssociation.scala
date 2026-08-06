package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserPostAssociation

object UserPostAssociation {
  lazy val table = TableQuery[UserPostAssociationTable]
}

case class UserPostAssociationConnection(
                                        subj: Identity[User],
                                       obj: Identity[Post],
                                       likes: Boolean,
                                       views: Boolean,
                                       watchCount: Int,
                                       lastInteractionDate: Option[Timestamp],
                                       createdAt: Timestamp
                                     )

class UserPostAssociationTable(tag: Tag) extends Table[UserPostAssociationConnection](tag, "user_post_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[Post]]("obj")
  def likes = column[Boolean]("likes")
  def views = column[Boolean]("views")
  def watchCount = column[Int]("watch_count")
  def lastInteractionDate = column[Option[Timestamp]]("last_interaction_date")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_user_post", (subj, obj))
  
  def idxSubj = index("idx_user_post_subj", subj)
  def idxObj = index("idx_user_post_obj", obj)
  def idxLikes = index("idx_user_post_likes", likes)
  def idxViews = index("idx_user_post_views", views)

  override def * : ProvenShape[UserPostAssociationConnection] = (subj, obj, likes, views, watchCount, lastInteractionDate, createdAt).mapTo[UserPostAssociationConnection]
} 