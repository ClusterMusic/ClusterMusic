package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class UserClusterAssociation

object UserClusterAssociation {
  lazy val table = TableQuery[UserClusterAssociationTable]
}

case class UserClusterAssociationConnection(
                                          subj: Identity[User],
                                          obj: Identity[Cluster],
                                          isFollowing: Boolean,
                                          joinDate: Option[Timestamp],
                                          lastInteractionDate: Option[Timestamp],
                                          createdAt: Timestamp
                                        )

class UserClusterAssociationTable(tag: Tag) extends Table[UserClusterAssociationConnection](tag, "user_cluster_association") {
  def subj = column[Identity[User]]("subj")
  def obj = column[Identity[Cluster]]("obj")
  def isFollowing = column[Boolean]("is_following")
  def joinDate = column[Option[Timestamp]]("join_date")
  def lastInteractionDate = column[Option[Timestamp]]("last_interaction_date")
  def createdAt = column[Timestamp]("created_at")

  def pk = primaryKey("pk_user_cluster", (subj, obj))
  
  def idxSubj = index("idx_user_cluster_subj", subj)
  def idxObj = index("idx_user_cluster_obj", obj)
  def idxIsFollowing = index("idx_user_cluster_following", isFollowing)

  override def * : ProvenShape[UserClusterAssociationConnection] = (subj, obj, isFollowing, joinDate, lastInteractionDate, createdAt).mapTo[UserClusterAssociationConnection]
} 