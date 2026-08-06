package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class Comment

object Comment {
  lazy val table = TableQuery[CommentTable]
}

case class CommentConnection(
                           id: Identity[Comment],
                           content: String,
                           author: Identity[User],
                           parent: Option[Identity[Comment]],
                           createdAt: Timestamp
                         )

class CommentTable(tag: Tag) extends Table[CommentConnection](tag, "comment") {
  def id = column[Identity[Comment]]("id", O.PrimaryKey, O.AutoInc)
  def content = column[String]("content")
  def author = column[Identity[User]]("author_id")
  def parent = column[Option[Identity[Comment]]]("parent_id")
  def createdAt = column[Timestamp]("created_at")
  
  
  def idxAuthor = index("idx_comment_author", author)
  def idxParent = index("idx_comment_parent", parent)
  def idxCreatedAt = index("idx_comment_created_at", createdAt)

  override def * : ProvenShape[CommentConnection] = (id, content, author, parent, createdAt).mapTo[CommentConnection]
} 