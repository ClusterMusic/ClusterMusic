package model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class Post

object Post {
  lazy val table = TableQuery[PostTable]
}

case class PostBlob(
                    id: Identity[Post],
                    caption: String,
                    song: SongBlob,
                    poster: UserBlob,
                    clique: CliqueBlob,
                    cluster: ClusterBlob,
                    createdAt: Timestamp,
                    rank: Double,
                    score: Double
                  )

case class PostConnection(
                           id: Identity[Post],
                           caption: Identity[Comment],
                           song: Identity[Song],
                           poster: Identity[User],
                           clique: Identity[Clique],
                           cluster: Identity[Cluster],
                           createdAt: Timestamp,
                           communityViews: Int,
                           globalViews: Int,
                           rank: Double,
                           score: Double
                         ) {
                          def blob(implicit database: Database, ec: ExecutionContext): Future[PostBlob] = {
                            val captionQuery = Comment.table.filter(_.id === caption).result.head
                            val songQuery = Song.table.filter(_.id === song).result.head
                            val posterQuery = User.table.filter(_.id === poster).result.head
                            val cliqueQuery = Clique.table.filter(_.id === clique).result.head
                            val clusterQuery = Cluster.table.filter(_.id === cluster).result.head

                            for {
                              captionConn <- database.run(captionQuery)
                              songBlob <- database.run(songQuery).flatMap(_.blob)
                              posterBlob <- database.run(posterQuery).flatMap(_.blob)
                              cliqueBlob <- database.run(cliqueQuery).flatMap(_.blob)
                              clusterBlob <- database.run(clusterQuery).flatMap(_.blob)
                            } yield PostBlob(
                              id,
                              captionConn.content,
                              songBlob,
                              posterBlob,
                              cliqueBlob,
                              clusterBlob,
                              createdAt,
                              rank,
                              score
                            )
                          }
                          def updateRank()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
                            val viewsQuery = UserPostAssociation.table.filter(a => a.obj === id && a.views).result
                            val likesQuery = UserPostAssociation.table.filter(a => a.obj === id && a.likes).result
                            val commentsQuery = Comment.table.filter(_.parent === caption).result

                            for {
                              views <- database.run(viewsQuery)
                              likes <- database.run(likesQuery)
                              comments <- database.run(commentsQuery)
                              viewsN = views.length
                              rewatchViewsN = views.map(_.watchCount).sum
                              likesN = likes.length
                              commentsN = comments.length
                              offset = java.time.ZoneOffset.UTC
                              timeDays = (LocalDateTime.now().toEpochSecond(offset) - createdAt.toInstant.getEpochSecond) / (24 * 60 * 60)
                              newRank = Scoring.postRank(viewsN, rewatchViewsN, likesN, commentsN, timeDays)
                              out <- database.run(sqlu"UPDATE post SET rank = $newRank WHERE id = ${id.value}")
                            } yield {
                              out > 0
                            }
                          }

                          def updatePostScore()(implicit database: Database, ec: ExecutionContext): Future[Boolean] = {
                            val viewsQuery = UserPostAssociation.table.filter(a => a.obj === id && a.views).result
                            val likesQuery = UserPostAssociation.table.filter(a => a.obj === id && a.likes).result
                            val commentsQuery = Comment.table.filter(_.parent === caption).result
                            val cliqueQuery = Clique.table.filter(_.id === clique).result.head
                            val posterQuery = User.table.filter(_.id === poster).result.head
                            val posterSongsQuery = Post.table.filter(_.poster === poster).map(_.song).distinct.result

                            for {
                              views <- database.run(viewsQuery)
                              likes <- database.run(likesQuery)
                              comments <- database.run(commentsQuery)
                              cliqueConn <- database.run(cliqueQuery)
                              posterConn <- database.run(posterQuery)
                              posterSongs <- database.run(posterSongsQuery)
                              viewsN = views.length
                              rewatchViewsN = views.map(_.watchCount).sum
                              likesN = likes.length
                              commentsN = comments.length
                              nSongs = posterSongs.length
                              promotedClique = true
                              communityBias = if(communityViews > 5) 1.0 else -1.0
                              globalBias = globalViews > 5
                              newScore = Scoring.postScore(promotedClique, communityBias, globalBias, viewsN, rewatchViewsN, likesN, commentsN, nSongs)
                              out <- database.run(sqlu"UPDATE post SET score = $newScore WHERE id = ${id.value}")
                            } yield {
                              out > 0
                            }
                          }
                         }

class PostTable(tag: Tag) extends Table[PostConnection](tag, "post") {
  def id = column[Identity[Post]]("id", O.PrimaryKey, O.AutoInc)
  def caption = column[Identity[Comment]]("caption_id", O.Unique)
  def song = column[Identity[Song]]("song_id")
  def poster = column[Identity[User]]("poster_id")
  def clique = column[Identity[Clique]]("clique_id")
  def cluster = column[Identity[Cluster]]("cluster_id")
  def createdAt = column[Timestamp]("created_at")
  def communityViews = column[Int]("community_views")
  def globalViews = column[Int]("global_views")
  def rank = column[Double]("rank")
  def score = column[Double]("score")

  def idxPoster = index("idx_post_poster", poster)
  def idxRank = index("idx_post_rank", rank)
  def idxClique = index("idx_post_clique", clique)
  def idxCluster = index("idx_post_cluster", cluster)
  def idxCreatedAt = index("idx_post_created_at", createdAt)

  override def * : ProvenShape[PostConnection] = (id, caption, song, poster, clique, cluster, createdAt, communityViews, globalViews, rank, score).mapTo[PostConnection]
}

