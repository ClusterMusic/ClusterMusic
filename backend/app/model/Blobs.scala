package model

import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object Blobs {

  val UnknownCommunity: CommunityBlob =
    CommunityBlob(Identity(-1), "Unknown", "unknown", "Unknown", Seq())

  private def communityBlobs(
    ids: Set[Identity[Community]]
  )(implicit database: Database, ec: ExecutionContext): Future[Map[Identity[Community], CommunityBlob]] =
    if (ids.isEmpty) Future.successful(Map.empty)
    else
      database
        .run(Community.table.filter(_.id inSet ids).result)
        .map(_.map(community => community.id -> community.blob).toMap)
        .recover { case _ => Map.empty }

  def users(
    users: Seq[UserConnection]
  )(implicit database: Database, ec: ExecutionContext): Future[Seq[UserBlob]] =
    if (users.isEmpty) Future.successful(Seq.empty)
    else
      communityBlobs(users.map(_.community).toSet).map { byId =>
        users.map(user => user.toBlob(byId.getOrElse(user.community, UnknownCommunity)))
      }

  def clusters(
    clusters: Seq[ClusterConnection]
  )(implicit database: Database, ec: ExecutionContext): Future[Seq[ClusterBlob]] =
    if (clusters.isEmpty) Future.successful(Seq.empty)
    else
      communityBlobs(clusters.map(_.parentCommunityId).toSet).map { byId =>
        clusters.map(cluster => cluster.toBlob(byId.getOrElse(cluster.parentCommunityId, UnknownCommunity)))
      }

  def posts(
    posts: Seq[PostConnection]
  )(implicit database: Database, ec: ExecutionContext): Future[Seq[PostBlob]] = {
    if (posts.isEmpty) return Future.successful(Seq.empty)

    val captionIds = posts.map(_.caption).toSet
    val songIds = posts.map(_.song).toSet
    val posterIds = posts.map(_.poster).toSet
    val cliqueIds = posts.map(_.clique).toSet
    val clusterIds = posts.map(_.cluster).toSet

    for {
      comments <- database.run(Comment.table.filter(_.id inSet captionIds).result)
      songs <- database.run(Song.table.filter(_.id inSet songIds).result)
      posters <- database.run(User.table.filter(_.id inSet posterIds).result)
      cliques <- database.run(Clique.table.filter(_.id inSet cliqueIds).result)
      clusters <- database.run(Cluster.table.filter(_.id inSet clusterIds).result)
      communityIds = (posters.map(_.community) ++ clusters.map(_.parentCommunityId)).toSet
      communityById <- communityBlobs(communityIds)
    } yield {
      val captionById = comments.map(comment => comment.id -> comment.content).toMap
      val songById = songs.map(song => song.id -> song.toBlob).toMap
      val cliqueById = cliques.map(clique => clique.id -> clique.toBlob).toMap
      val posterById = posters.map { user =>
        user.id -> user.toBlob(communityById.getOrElse(user.community, UnknownCommunity))
      }.toMap
      val clusterById = clusters.map { cluster =>
        cluster.id -> cluster.toBlob(communityById.getOrElse(cluster.parentCommunityId, UnknownCommunity))
      }.toMap

      posts.flatMap { post =>
        for {
          song <- songById.get(post.song)
          poster <- posterById.get(post.poster)
          clique <- cliqueById.get(post.clique)
          cluster <- clusterById.get(post.cluster)
        } yield PostBlob(
          post.id,
          captionById.getOrElse(post.caption, ""),
          song,
          poster,
          clique,
          cluster,
          post.createdAt,
          post.rank,
          post.score
        )
      }
    }
  }
}
