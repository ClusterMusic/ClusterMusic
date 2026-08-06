package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class Cluster

object Cluster {
  lazy val table = TableQuery[ClusterTable]
}

case class ClusterBlob(
                      id: Identity[Cluster],
                      image: String,
                      title: String,
                      community: CommunityBlob,
                      tags: Seq[String],
                      )

case class ClusterProfile(
                           id: Identity[Cluster],
                           image: String,
                           title: String,
                           community: CommunityBlob,
                           tags: Seq[String],
                           followers: Int
                         )


case class ClusterConnection(
                           id: Identity[Cluster],
                           title: String,
                           parentClusterId: Option[Identity[Cluster]],
                           parentCommunityId: Identity[Community],
                           createdAt: Timestamp
                         ) {
  def blob(implicit database: Database, ec: ExecutionContext): Future[ClusterBlob] = {
    val communityQuery = Community.table.filter(_.id === parentCommunityId).result.head
    database.run(communityQuery).map { community =>
      ClusterBlob(id, Temp.imageURL, title, community.blob, Temp.clusterTags)
    }.recover(_ => ClusterBlob(id, Temp.imageURL, title, CommunityBlob(Identity(-1), "Unknown", "unknown", "Unknown", Seq()), Temp.clusterTags))
  }

  def profile(implicit database: Database, ec: ExecutionContext): Future[ClusterProfile] = {
    val communityQuery = Community.table.filter(_.id === parentCommunityId).result.head
    val followersQuery = UserClusterAssociation.table.filter(assoc =>
      assoc.obj === id && assoc.isFollowing === true
    ).length.result
    for {
      communityblob <- database.run(communityQuery).map(_.blob).recover(_ => CommunityBlob(Identity(-1), "Unknown", "unknown", "Unknown", Seq()))
      followers <- database.run(followersQuery).recover(_ => -1)
    } yield ClusterProfile(id, Temp.imageURL, title, communityblob, Temp.clusterTags, followers)
  }
}

class ClusterTable(tag: Tag) extends Table[ClusterConnection](tag, "cluster") {
  def id = column[Identity[Cluster]]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def parentClusterId = column[Option[Identity[Cluster]]]("parent_cluster_id")
  def parentCommunityId = column[Identity[Community]]("parent_community_id")
  def createdAt = column[Timestamp]("created_at")
  
  def idxParentCluster = index("idx_cluster_parent", parentClusterId)
  def idxParentCommunity = index("idx_cluster_community", parentCommunityId)
  def idxCreatedAt = index("idx_cluster_created_at", createdAt)

  override def * : ProvenShape[ClusterConnection] = (id, title, parentClusterId, parentCommunityId, createdAt).mapTo[ClusterConnection]
} 