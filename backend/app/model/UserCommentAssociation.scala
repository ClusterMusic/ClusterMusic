package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserCommentAssociation

object UserCommentAssociation {
  lazy val table = TableQuery[UserCommentAssociationTable]
}

case class UserCommentAssociationConnection(
                                          subj: Identity[User],
                                          obj: Identity[Comment],
                                          likes: Boolean,
                                          views: Boolean,
                                          lastInteractionDate: Option[Timestamp],
                                          createdAt: Timestamp
                                        )

class UserCommentAssociationTable(tag: Tag) extends Table[UserCommentAssociationConnection](tag, "user_comment_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[Comment]]("obj")
  def likes = column[Boolean]("likes")
  def views = column[Boolean]("views")
  def lastInteractionDate = column[Option[Timestamp]]("last_interaction_date")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_user_comment", (subj, obj))
  
  def idxSubj = index("idx_user_comment_subj", subj)
  def idxObj = index("idx_user_comment_obj", obj)
  def idxLikes = index("idx_user_comment_likes", likes)
  def idxViews = index("idx_user_comment_views", views)

  override def * : ProvenShape[UserCommentAssociationConnection] = (subj, obj, likes, views, lastInteractionDate, createdAt).mapTo[UserCommentAssociationConnection]
} 