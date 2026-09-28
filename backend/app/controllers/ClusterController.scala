package controllers

import auth.{Authorization, OptionalUserAction, RefreshToken, UserAuthorizedAction}
import model.JsonFormats._

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import model._
import play.api.libs.json.JsError.toJson

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.MySQLProfile.api._

import java.sql.Timestamp
import java.time.LocalDateTime


@Singleton
class ClusterController @Inject()(
  val controllerComponents: ControllerComponents, 
  database: Database,
  optionalUserAction: OptionalUserAction,
  authorizedAction: UserAuthorizedAction,
  @Named("database") dbEc: ExecutionContext,  
  ec: ExecutionContext
) extends BaseController {


  implicit val db: Database = database

  implicit val databaseEc: ExecutionContext = dbEc

  implicit val identityWrites: Writes[Identity[_]] = Writes[Identity[_]] { identity =>
    Json.obj("id" -> identity.value)
  }

  def community(
               id: Identity[Community],
               format: String
               ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = Community.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(community) =>
        format match {
          case "database" =>
            if(auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(community)))
            }
          case "blob" =>
            Future.successful(Ok(Json.toJson(community.blob)))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Cluster not found", "details" -> s"Cluster with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def cluster(
               id: Identity[Cluster],
               format: String
             ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = Cluster.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(cluster) =>
        format match {
          case "database" =>
            if (auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(cluster)))
            }
          case "blob" =>
            cluster.blob.map { blob =>
              Ok(Json.toJson(blob))
            }
          case "profile" =>
            cluster.profile.map { profile =>
              Ok(Json.toJson(profile))
            }
          case _ =>
            Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Cluster not found", "details" -> s"Cluster with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def clique(
             id: Identity[Clique],
             format: String
           ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = Clique.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(clique) =>
        format match {
          case "database" =>
            if (auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(clique)))
            }
          case "blob" =>
            clique.blob.map { blob =>
              Ok(Json.toJson(blob))
            }
          case "profile" =>
            clique.profile.map { profile =>
              Ok(Json.toJson(profile))
            }
          case _ =>
            Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Clique not found", "details" -> s"Clique with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def clusters(
                format: String,
                community: Option[Identity[Community]] = None,
                followed: Option[Identity[User]] = None,
                parent: Option[Identity[Cluster]] = None
              ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>

    val baseQuery = Cluster.table.filterOpt(community)(
      (table, community) => table.parentCommunityId === community
    ).filterOpt(parent)(
      (table, parent) => table.parentClusterId.map(_ === parent).getOrElse(false)
    )

    val finalQuery = followed match {
      case Some(user) =>
        baseQuery.join(UserClusterAssociation.table.filter(assoc =>
          assoc.subj === user && assoc.isFollowing === true
        )).on(_.id === _.obj).map(_._1)
      case None => baseQuery
    }


    database.run(finalQuery.result).flatMap { clusters =>
      format match {
        case "database" =>
          if (auth_request.auth != Authorization.Admin) {
            Future.successful(Unauthorized(Json.obj(
              "error" -> "Authorization header required",
              "code" -> "NO_AUTH_HEADER"
            )))
          } else {
            Future.successful(Ok(Json.toJson(clusters)))
          }
        case "blob" =>
          val blobs = Blobs.clusters(clusters)
          blobs.map { blobs => Ok(Json.toJson(blobs)) }
        case "profile" =>
          val profiles = Future.sequence(clusters.map(_.profile))
          profiles.map { profiles => Ok(Json.toJson(profiles)) }

        case _ =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))

      }

    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)

  }

  def user(
            id: Identity[User],
            format: String
          ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = User.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(user) =>
        format match {
          case "database" =>
            if (auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(user)))
            }
          case "blob" =>
            user.blob.map { blob =>
              Ok(Json.toJson(blob))
            }
          case "profile" =>
            user.profile.map { profile =>
              Ok(Json.toJson(profile))
            }
          case _ =>
            Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "User not found", "details" -> s"User with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def users(
             format: String,
             community: Option[Identity[Community]] = None,
             followed: Option[Identity[User]] = None,
             following: Option[Identity[User]] = None,
             followingCluster: Option[Identity[Cluster]] = None,
             followingClique: Option[Identity[Clique]] = None,
             memberClique: Option[Identity[Clique]] = None,
             likedPost: Option[Identity[Post]] = None,
             viewedPost: Option[Identity[Post]] = None,
             achievement: Option[Identity[UserAchievement]] = None,
             limit: Int = 20
           ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>

    val baseQuery = User.table.filterOpt(community)(
      (table, community) => table.community === community
    )

    val queryWithFollowed = followed match {
      case Some(user) =>
        baseQuery.join(UserUserAssociation.table.filter(assoc =>
          assoc.obj === user && assoc.follows === true
        )).on(_.id === _.subj).map(_._1)
      case None => baseQuery
    }

    val queryWithFollowing = following match {
      case Some(user) =>
        queryWithFollowed.join(UserUserAssociation.table.filter(assoc =>
          assoc.subj === user && assoc.follows === true
        )).on(_.id === _.obj).map(_._1)
      case None => queryWithFollowed
    }

    val queryWithFollowingCluster = followingCluster match {
      case Some(cluster) =>
        queryWithFollowing.join(UserClusterAssociation.table.filter(assoc =>
          assoc.obj === cluster && assoc.isFollowing === true
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithFollowing
    }

    val queryWithFollowingClique = followingClique match {
      case Some(clique) =>
        queryWithFollowingCluster.join(UserCliqueAssociation.table.filter(assoc =>
          assoc.obj === clique
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithFollowingCluster
    }

    val queryWithMemberClique = memberClique match {
      case Some(clique) =>
        queryWithFollowingClique.join(UserCliqueAssociation.table.filter(assoc =>
          assoc.obj === clique && assoc.joinDate.isDefined
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithFollowingClique
    }

    val queryWithLikedPost = likedPost match {
      case Some(post) =>
        queryWithMemberClique.join(UserPostAssociation.table.filter(assoc =>
          assoc.obj === post && assoc.likes === true
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithMemberClique
    }

    val queryWithViewedPost = viewedPost match {
      case Some(post) =>
        queryWithLikedPost.join(UserPostAssociation.table.filter(assoc =>
          assoc.obj === post && assoc.views === true
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithLikedPost
    }

    val finalQuery = achievement match {
      case Some(ach) =>
        queryWithViewedPost.join(UserAchievementAssociation.table.filter(assoc =>
          assoc.obj === ach
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithViewedPost
    }

    val boundedLimit = math.max(1, math.min(limit, 100))

    database.run(finalQuery.take(boundedLimit).result).flatMap { users =>
      format match {
        case "database" =>
          if (auth_request.auth != Authorization.Admin) {
            Future.successful(Unauthorized(Json.obj(
              "error" -> "Authorization header required",
              "code" -> "NO_AUTH_HEADER"
            )))
          } else {
            Future.successful(Ok(Json.toJson(users)))
          }
        case "blob" =>
          val blobs = Blobs.users(users)
          blobs.map { blobs =>
            Ok(Json.toJson(blobs)) }
        case "profile" =>
          val profiles = Future.sequence(users.map(_.profile))
          profiles.map { profiles => Ok(Json.toJson(profiles)) }

        case _ =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))

      }

    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)

  }

  def cliques(
              format: String,
              member: Option[Identity[User]] = None,
              followed: Option[Identity[User]] = None,
              hasPost: Option[Identity[Post]] = None,
              achievement: Option[Identity[CliqueAchievement]] = None,
              cluster: Option[Identity[Cluster]] = None
            ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>

    val baseQuery = Clique.table

    val queryWithMember = member match {
      case Some(user) =>
        baseQuery.join(UserCliqueAssociation.table.filter(assoc =>
          assoc.subj === user && (assoc.memberRole === CliqueMemberRole.Member || 
                                 assoc.memberRole === CliqueMemberRole.Moderator || 
                                 assoc.memberRole === CliqueMemberRole.Admin)
        )).on(_.id === _.obj).map(_._1)
      case None => baseQuery
    }

    val queryWithFollowed = followed match {
      case Some(user) =>
        queryWithMember.join(UserCliqueAssociation.table.filter(assoc =>
          assoc.subj === user && assoc.memberRole === CliqueMemberRole.Following
        )).on(_.id === _.obj).map(_._1)
      case None => queryWithMember
    }

    val queryWithPost = hasPost match {
      case Some(post) =>
        queryWithFollowed.join(Post.table.filter(_.id === post)).on(_.id === _.clique).map(_._1)
      case None => queryWithFollowed
    }

    val finalQuery = achievement match {
      case Some(ach) =>
        queryWithPost.join(CliqueAchievementAssociation.table.filter(assoc =>
          assoc.obj === ach
        )).on(_.id === _.subj).map(_._1)
      case None => queryWithPost
    }

    database.run(finalQuery.result).flatMap { cliques =>
      format match {
        case "database" =>
          if (auth_request.auth != Authorization.Admin) {
            Future.successful(Unauthorized(Json.obj(
              "error" -> "Authorization header required",
              "code" -> "NO_AUTH_HEADER"
            )))
          } else {
            Future.successful(Ok(Json.toJson(cliques)))
          }
        case "blob" =>
          val blobs = Future.sequence(cliques.map(_.blob))
          blobs.map { blobs => Ok(Json.toJson(blobs)) }
        case "profile" =>
          val profiles = Future.sequence(cliques.map(_.profile))
          profiles.map { profiles => Ok(Json.toJson(profiles)) }

        case _ =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))

      }

    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)

  }

  def song(
            id: Identity[Song],
            format: String
          ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = Song.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(song) =>
        format match {
          case "database" =>
            if (auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(song)))
            }
          case "blob" =>
            song.blob.map { blob =>
              Ok(Json.toJson(blob))
            }
          case _ =>
            Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Song not found", "details" -> s"Song with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def songs(
             format: String,
             title: Option[String] = None,
             spotifyId: Option[Identity[SpotifySong]] = None,
             appleMusicId: Option[Identity[AppleMusicSong]] = None
           ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>

    val baseQuery = Song.table
      .filterOpt(title)((table, title) => table.title.toLowerCase like s"%${title.toLowerCase}%")
      .filterOpt(spotifyId)((table, spotifyId) => table.spotify_id === spotifyId)
      .filterOpt(appleMusicId)((table, appleMusicId) => table.apple_music_id === appleMusicId)

    database.run(baseQuery.result).flatMap { songs =>
      format match {
        case "database" =>
          if (auth_request.auth != Authorization.Admin) {
            Future.successful(Unauthorized(Json.obj(
              "error" -> "Authorization header required",
              "code" -> "NO_AUTH_HEADER"
            )))
          } else {
            Future.successful(Ok(Json.toJson(songs)))
          }
        case "blob" =>
          val blobs = Future.sequence(songs.map(_.blob))
          blobs.map { blobs => Ok(Json.toJson(blobs)) }
        case _ =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
      }
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  

  def post(
            id: Identity[Post],
            format: String
          ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    val query = Post.table.filter(_.id === id).result.headOption
    database.run(query).flatMap {
      case Some(post) =>
        format match {
          case "database" =>
            if (auth_request.auth != Authorization.Admin) {
              Future.successful(Unauthorized(Json.obj(
                "error" -> "Authorization header required",
                "code" -> "NO_AUTH_HEADER"
              )))
            } else {
              Future.successful(Ok(Json.toJson(post)))
            }
          case "blob" =>
            post.blob.map { blob =>
              Ok(Json.toJson(blob))
            }
          case _ =>
            Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Post not found", "details" -> s"Post with id ${id.value} not found")))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def posts(
             format: String,
             poster: Option[Identity[User]] = None,
             clique: Option[Identity[Clique]] = None,
             cluster: Option[Identity[Cluster]] = None,
             song: Option[Identity[Song]] = None,
             likedBy: Option[Identity[User]] = None,
             viewedBy: Option[Identity[User]] = None,
             limit: Int = 20
           ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>

    val baseQuery = Post.table
      .filterOpt(poster)((table, poster) => table.poster === poster)
      .filterOpt(clique)((table, clique) => table.clique === clique)
      .filterOpt(cluster)((table, cluster) => table.cluster === cluster)
      .filterOpt(song)((table, song) => table.song === song)

    val queryWithLikedBy = likedBy match {
      case Some(user) =>
        val likedPostIds = UserPostAssociation.table
          .filter(assoc => assoc.subj === user && assoc.likes === true)
          .map(_.obj)
        baseQuery.filter(_.id.in(likedPostIds))
      case None => baseQuery
    }

    val finalQuery = viewedBy match {
      case Some(user) =>
        val viewedPostIds = UserPostAssociation.table
          .filter(assoc => assoc.subj === user && assoc.views === true)
          .map(_.obj)
        queryWithLikedBy.filter(_.id.in(viewedPostIds))
      case None => queryWithLikedBy
    }

    val boundedLimit = math.max(1, math.min(limit, 100))

    database.run(finalQuery.sortBy(_.rank.desc).take(boundedLimit).result).flatMap { posts =>
      format match {
        case "database" =>
          if (auth_request.auth != Authorization.Admin) {
            Future.successful(Unauthorized(Json.obj(
              "error" -> "Authorization header required",
              "code" -> "NO_AUTH_HEADER"
            )))
          } else {
            Future.successful(Ok(Json.toJson(posts)))
          }
        case "blob" =>
          val blobs = Blobs.posts(posts)
          blobs.map { blobs => Ok(Json.toJson(blobs)) }
        case _ =>
          Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> "Invalid layout")))
      }
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)
  }

  def userRadio(
                 userId: Identity[User],
                 count: Int
               ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = UserRadio(auth_request.user, userId)
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def cliqueRadio(
                   cliqueId: Identity[Clique],
                   count: Int
                 ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = CliqueRadio(auth_request.user, cliqueId)
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def clusterRadio(
                    clusterId: Identity[Cluster],
                    count: Int
                  ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = ClusterRadio(auth_request.user, clusterId)
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def forYouRadio(
                   count: Int
                 ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = ForYouRadio(auth_request.user, ())
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def followUser(
                id: Identity[User]
              ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    if (auth_request.user == id) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Invalid operation", 
        "details" -> "Users cannot follow themselves"
      )))
    } else {
      val userQuery = User.table.filter(_.id === id).result.headOption
      
      database.run(userQuery).flatMap {
        case Some(targetUser) => 

          val existingRelationQuery = UserUserAssociation.table
            .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.follows === true)
            .result.headOption
            
          database.run(existingRelationQuery).flatMap {
            case Some(_) => 
              Future.successful(Conflict(Json.obj(
                "error" -> "Already following", 
                "details" -> s"User is already following user with id ${id.value}"
              )))
            case None =>
              val now = Timestamp.valueOf(LocalDateTime.now())
              val relation = UserUserAssociationConnection(
                auth_request.user,
                id, 
                follows = true,
                lastInteractionDate = Some(now),
                createdAt = now
              )
              
              database.run(UserUserAssociation.table += relation).map {
                case 0 => BadRequest(Json.obj(
                  "error" -> "Failed to follow user", 
                  "details" -> "Database operation failed"
                ))
                case _ => Ok(Json.obj(
                  "message" -> "User followed successfully",
                  "followedUserId" -> id.value
                ))
              }
          }
        case None => 
          Future.successful(NotFound(Json.obj(
            "error" -> "User not found", 
            "details" -> s"User with id ${id.value} not found"
          )))
      }.recover {
        case ex: Exception =>
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "Database error", 
            "details" -> "An unexpected error occurred"
          ))
      }(databaseEc)
    }
  }

  def followClique(
                    id: Identity[Clique]
                  ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val cliqueQuery = Clique.table.filter(_.id === id).result.headOption
    
    database.run(cliqueQuery).flatMap {
      case Some(targetClique) => 
        val existingRelationQuery = UserCliqueAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
          .result.headOption
          
        database.run(existingRelationQuery).flatMap {
          case Some(existing) if existing.memberRole == CliqueMemberRole.Following => 
            Future.successful(Conflict(Json.obj(
              "error" -> "Already following", 
              "details" -> s"User is already following clique with id ${id.value}"
            )))
          case Some(existing) if existing.memberRole.id >= CliqueMemberRole.Member.id => 
            Future.successful(Conflict(Json.obj(
              "error" -> "Already member", 
              "details" -> s"User is already a member of clique with id ${id.value}"
            )))
          case Some(_) =>
            val updateQuery = UserCliqueAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .map(_.memberRole)
              .update(CliqueMemberRole.Following)
              
            database.run(updateQuery).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to follow clique", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Clique followed successfully",
                "followedCliqueId" -> id.value
              ))
            }
          case None =>
            val now = Timestamp.valueOf(LocalDateTime.now())
            val relation = UserCliqueAssociationConnection(
              auth_request.user,
              id, 
              CliqueMemberRole.Following,
              joinDate = None,
              lastInteractionDate = Some(now),
              createdAt = now
            )
            
            database.run(UserCliqueAssociation.table += relation).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to follow clique", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Clique followed successfully",
                "followedCliqueId" -> id.value
              ))
            }
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Clique not found", 
          "details" -> s"Clique with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def followCluster(
                     id: Identity[Cluster]
                   ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val clusterQuery = Cluster.table.filter(_.id === id).result.headOption
    
    database.run(clusterQuery).flatMap {
      case Some(targetCluster) => 
        val existingRelationQuery = UserClusterAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.isFollowing === true)
          .result.headOption
          
        database.run(existingRelationQuery).flatMap {
          case Some(_) => 
            Future.successful(Conflict(Json.obj(
              "error" -> "Already following", 
              "details" -> s"User is already following cluster with id ${id.value}"
            )))
          case None =>
            val anyExistingRelationQuery = UserClusterAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .result.headOption
              
            database.run(anyExistingRelationQuery).flatMap {
              case Some(existing) =>
                val updateQuery = UserClusterAssociation.table
                  .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
                  .map(_.isFollowing)
                  .update(true)
                  
                database.run(updateQuery).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to follow cluster", 
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Cluster followed successfully",
                    "followedClusterId" -> id.value
                  ))
                }
              case None =>
                val now = Timestamp.valueOf(LocalDateTime.now())
                val relation = UserClusterAssociationConnection(
                  auth_request.user,
                  id, 
                  isFollowing = true,
                  joinDate = None,
                  lastInteractionDate = Some(now),
                  createdAt = now
                )
                
                database.run(UserClusterAssociation.table += relation).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to follow cluster", 
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Cluster followed successfully",
                    "followedClusterId" -> id.value
                  ))
                }
            }
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Cluster not found", 
          "details" -> s"Cluster with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def createClique(
                    name: String,
                    biography: String,
                  ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    val roomId = 0
    val newClique = CliqueConnection(Identity(0),name, biography, roomId, Timestamp.valueOf(LocalDateTime.now()), 0.0, 0.0)
    val now = Timestamp.valueOf(LocalDateTime.now())
    val ownerRelation = UserCliqueAssociationConnection(auth_request.user, newClique.id, CliqueMemberRole.Admin, Some(now), Some(now), now)

    database.run(Clique.table += newClique).flatMap {
      case 0 => Future.successful(BadRequest(Json.obj("error" -> "Failed to create clique", "details" -> "Failed to create clique")))
      case _ => newClique.profile.map(profile => (Created(Json.toJson(profile))))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Database error", "details" -> "Details hidden"))
    }(databaseEc)

  }

  def globalRadio(
                   count: Int
                 ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = GlobalRadio(auth_request.user, ())
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def communityRadio(
                      communityId: Identity[Community],
                      count: Int
                    ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = CommunityRadio(auth_request.user, communityId)
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def followingRadio(
                      count: Int
                    ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    try {
      val radio = FollowingRadio(auth_request.user, ())
      val posts = radio.get(count)
      Blobs.posts(posts).map { postBlobs =>
        Ok(Json.toJson(postBlobs))
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Future.successful(InternalServerError(Json.obj("error" -> "Radio error", "details" -> "Failed to generate radio feed")))
    }
  }

  def unfollowUser(
                    id: Identity[User]
                  ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    if (auth_request.user == id) {
      Future.successful(BadRequest(Json.obj(
        "error" -> "Invalid operation", 
        "details" -> "Users cannot unfollow themselves"
      )))
    } else {
      val deleteQuery = UserUserAssociation.table
        .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.follows === true)
        .delete
        
      database.run(deleteQuery).map {
        case 0 => NotFound(Json.obj(
          "error" -> "Not following", 
          "details" -> s"User is not following user with id ${id.value}"
        ))
        case _ => Ok(Json.obj(
          "message" -> "User unfollowed successfully",
          "unfollowedUserId" -> id.value
        ))
      }.recover {
        case ex: Exception =>
          ex.printStackTrace()
          InternalServerError(Json.obj(
            "error" -> "Database error", 
            "details" -> "An unexpected error occurred"
          ))
      }(databaseEc)
    }
  }

  def likePost(
                id: Identity[Post]
              ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    val postQuery = Post.table.filter(_.id === id).result.headOption
    
    database.run(postQuery).flatMap {
      case Some(post) => 
        val existingLikeQuery = UserPostAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.likes === true)
          .result.headOption
          
        database.run(existingLikeQuery).flatMap {
          case Some(_) => 
            Future.successful(Conflict(Json.obj(
              "error" -> "Already liked", 
              "details" -> s"User has already liked post with id ${id.value}"
            )))
          case None =>
            val now = Timestamp.valueOf(LocalDateTime.now())
            val anyExistingRelationQuery = UserPostAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .result.headOption

            database.run(anyExistingRelationQuery).flatMap {
              case Some(_) =>
                val updateQuery = UserPostAssociation.table
                  .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
                  .map(assoc => (assoc.likes, assoc.lastInteractionDate))
                  .update((true, Some(now)))

                database.run(updateQuery).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to like post",
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Post liked successfully",
                    "likedPostId" -> id.value
                  ))
                }
              case None =>
                val relation = UserPostAssociationConnection(
                  auth_request.user,
                  id,
                  likes = true,
                  views = false,
                  watchCount = 0,
                  lastInteractionDate = Some(now),
                  createdAt = now
                )

                database.run(UserPostAssociation.table += relation).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to like post",
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Post liked successfully",
                    "likedPostId" -> id.value
                  ))
                }
            }
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Post not found", 
          "details" -> s"Post with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def viewPost(
                id: Identity[Post]
              ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    val postQuery = Post.table.filter(_.id === id).result.headOption
    
    database.run(postQuery).flatMap {
      case Some(post) => 
        val existingViewQuery = UserPostAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.views === true)
          .result.headOption
          
        database.run(existingViewQuery).flatMap {
          case Some(existing) =>
            val updateQuery = UserPostAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .map(assoc => (assoc.watchCount, assoc.lastInteractionDate))
              .update((existing.watchCount + 1, Some(Timestamp.valueOf(LocalDateTime.now()))))
              
            database.run(updateQuery).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to update view count", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Post view updated successfully",
                "viewedPostId" -> id.value,
                "watchCount" -> (existing.watchCount + 1)
              ))
            }
          case None =>
            val now = Timestamp.valueOf(LocalDateTime.now())
            val anyExistingRelationQuery = UserPostAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .result.headOption

            database.run(anyExistingRelationQuery).flatMap {
              case Some(_) =>
                val updateQuery = UserPostAssociation.table
                  .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
                  .map(assoc => (assoc.views, assoc.watchCount, assoc.lastInteractionDate))
                  .update((true, 1, Some(now)))

                database.run(updateQuery).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to record view",
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Post viewed successfully",
                    "viewedPostId" -> id.value,
                    "watchCount" -> 1
                  ))
                }
              case None =>
                val relation = UserPostAssociationConnection(
                  auth_request.user,
                  id,
                  likes = false,
                  views = true,
                  watchCount = 1,
                  lastInteractionDate = Some(now),
                  createdAt = now
                )

                database.run(UserPostAssociation.table += relation).map {
                  case 0 => BadRequest(Json.obj(
                    "error" -> "Failed to record view",
                    "details" -> "Database operation failed"
                  ))
                  case _ => Ok(Json.obj(
                    "message" -> "Post viewed successfully",
                    "viewedPostId" -> id.value,
                    "watchCount" -> 1
                  ))
                }
            }
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Post not found", 
          "details" -> s"Post with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def unfollowClique(
                      id: Identity[Clique]
                    ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val cliqueQuery = Clique.table.filter(_.id === id).result.headOption
    
    database.run(cliqueQuery).flatMap {
      case Some(targetClique) => 
        val deleteQuery = UserCliqueAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.memberRole === CliqueMemberRole.Following)
          .delete
          
        database.run(deleteQuery).map {
          case 0 => NotFound(Json.obj(
            "error" -> "Not following", 
            "details" -> s"User is not following clique with id ${id.value}"
          ))
          case _ => Ok(Json.obj(
            "message" -> "Clique unfollowed successfully",
            "unfollowedCliqueId" -> id.value
          ))
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Clique not found", 
          "details" -> s"Clique with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def joinClique(
                  id: Identity[Clique]
                ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val cliqueQuery = Clique.table.filter(_.id === id).result.headOption
    
    database.run(cliqueQuery).flatMap {
      case Some(targetClique) => 
        val existingRelationQuery = UserCliqueAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
          .result.headOption
          
        database.run(existingRelationQuery).flatMap {
          case Some(existing) if existing.memberRole.id >= CliqueMemberRole.Member.id => 
            Future.successful(Conflict(Json.obj(
              "error" -> "Already member", 
              "details" -> s"User is already a member of clique with id ${id.value}"
            )))
          case Some(existing) =>
            val now = Timestamp.valueOf(LocalDateTime.now())
            val updateQuery = UserCliqueAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .map(assoc => (assoc.memberRole, assoc.joinDate))
              .update((CliqueMemberRole.Member, Some(now)))
              
            database.run(updateQuery).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to join clique", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Clique joined successfully",
                "joinedCliqueId" -> id.value
              ))
            }
          case None =>
            val now = Timestamp.valueOf(LocalDateTime.now())
            val relation = UserCliqueAssociationConnection(
              auth_request.user,
              id, 
              CliqueMemberRole.Member,
              joinDate = Some(now),
              lastInteractionDate = Some(now),
              createdAt = now
            )
            
            database.run(UserCliqueAssociation.table += relation).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to join clique", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Clique joined successfully",
                "joinedCliqueId" -> id.value
              ))
            }
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Clique not found", 
          "details" -> s"Clique with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def leaveClique(
                   id: Identity[Clique]
                 ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val cliqueQuery = Clique.table.filter(_.id === id).result.headOption
    
    database.run(cliqueQuery).flatMap {
      case Some(targetClique) => 
        val existingRelationQuery = UserCliqueAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
          .result.headOption
          
        database.run(existingRelationQuery).flatMap {
          case Some(existing) if existing.memberRole == CliqueMemberRole.Admin => 
            Future.successful(BadRequest(Json.obj(
              "error" -> "Cannot leave", 
              "details" -> "Admin cannot leave clique. Transfer ownership first."
            )))
          case Some(existing) if existing.memberRole.id >= CliqueMemberRole.Member.id =>
            val updateQuery = UserCliqueAssociation.table
              .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id)
              .map(assoc => (assoc.memberRole, assoc.joinDate))
              .update((CliqueMemberRole.Following, None))
              
            database.run(updateQuery).map {
              case 0 => BadRequest(Json.obj(
                "error" -> "Failed to leave clique", 
                "details" -> "Database operation failed"
              ))
              case _ => Ok(Json.obj(
                "message" -> "Clique left successfully",
                "leftCliqueId" -> id.value
              ))
            }
          case Some(_) => 
            Future.successful(BadRequest(Json.obj(
              "error" -> "Not a member", 
              "details" -> s"User is not a member of clique with id ${id.value}"
            )))
          case None =>
            Future.successful(NotFound(Json.obj(
              "error" -> "No relationship", 
              "details" -> s"User has no relationship with clique with id ${id.value}"
            )))
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Clique not found", 
          "details" -> s"Clique with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def unfollowCluster(
                       id: Identity[Cluster]
                     ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val clusterQuery = Cluster.table.filter(_.id === id).result.headOption
    
    database.run(clusterQuery).flatMap {
      case Some(targetCluster) => 
        val deleteQuery = UserClusterAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.isFollowing === true)
          .delete
          
        database.run(deleteQuery).map {
          case 0 => NotFound(Json.obj(
            "error" -> "Not following", 
            "details" -> s"User is not following cluster with id ${id.value}"
          ))
          case _ => Ok(Json.obj(
            "message" -> "Cluster unfollowed successfully",
            "unfollowedClusterId" -> id.value
          ))
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Cluster not found", 
          "details" -> s"Cluster with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  def unlikePost(
                  id: Identity[Post]
                ): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    
    val postQuery = Post.table.filter(_.id === id).result.headOption
    
    database.run(postQuery).flatMap {
      case Some(post) => 
        val deleteQuery = UserPostAssociation.table
          .filter(assoc => assoc.subj === auth_request.user && assoc.obj === id && assoc.likes === true)
          .delete
          
        database.run(deleteQuery).map {
          case 0 => NotFound(Json.obj(
            "error" -> "Not liked", 
            "details" -> s"User has not liked post with id ${id.value}"
          ))
          case _ => Ok(Json.obj(
            "message" -> "Post unliked successfully",
            "unlikedPostId" -> id.value
          ))
        }
      case None => 
        Future.successful(NotFound(Json.obj(
          "error" -> "Post not found", 
          "details" -> s"Post with id ${id.value} not found"
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj(
          "error" -> "Database error", 
          "details" -> "An unexpected error occurred"
        ))
    }(databaseEc)
  }

  case class CreatePostRequest(
                                caption: String,
                                songId: Identity[Song],
                                cliqueId: Identity[Clique],
                                clusterId: Identity[Cluster]
                              )


  def createPost(): Action[JsValue] = authorizedAction.async(parse.json) { implicit auth_request =>
    implicit val createPostRequestReads: Reads[CreatePostRequest] = Json.reads[CreatePostRequest]
    
    auth_request.body.validate[CreatePostRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format"))),
      postReq => {
        val now = Timestamp.valueOf(LocalDateTime.now())
        val newComment = CommentConnection(
          id = Identity[Comment](0),
          content = postReq.caption,
          author = auth_request.user,
          parent = None,
          createdAt = now
        )
        
        val insertCommentAction = (Comment.table returning Comment.table.map(_.id)) += newComment
        
        val insertCommentAndCommunity = for {
          commentId <- insertCommentAction
          posterCommunity <- User.table.filter(_.id === auth_request.user).map(_.community).result.head
        } yield (commentId, posterCommunity)

        database.run(insertCommentAndCommunity).flatMap { case (commentId, posterCommunity) =>
          val newPost = PostConnection(
            id = Identity[Post](0),
            caption = commentId,
            song = postReq.songId,
            poster = auth_request.user,
            clique = postReq.cliqueId,
            cluster = postReq.clusterId,
            community = posterCommunity,
            createdAt = now,
            communityViews = 0,
            globalViews = 0,
            rank = 0.0,
            score = 0.0
          )
          
          val insertPostAction = (Post.table returning Post.table.map(_.id)) += newPost
          
          database.run(insertPostAction).flatMap { postId =>
            val createdPost = newPost.copy(id = postId)
            createdPost.blob.map { postBlob =>
              Created(Json.toJson(postBlob))
            }
          }
        }.recover {
          case ex: Exception =>
            ex.printStackTrace()
            InternalServerError(Json.obj("error" -> "Failed to create post", "details" -> "Database operation failed"))
        }(databaseEc)
      }
    )
  }

  case class SearchResult(
                         id: Int,
                         name: String,
                         typ: String,
                         image: String,
                         subtitle: String
                         )

  implicit val searchResultWrites: Writes[SearchResult] = Json.writes[SearchResult]

  def search(
              query: String,
              limit: Int = 20
            ): Action[AnyContent] = optionalUserAction.async { implicit auth_request =>
    if (query.trim.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Query cannot be empty")))
    } else {
      val searchTerm = s"%${query.toLowerCase}%"
      
      // Search users by username
      val usersQuery = User.table
        .filter(_.username.toLowerCase like searchTerm)
        .take(limit)
        .result
      
      // Search clusters by title
      val clustersQuery = Cluster.table
        .filter(_.title.toLowerCase like searchTerm)
        .take(limit)
        .result
      
      // Search cliques by name
      val cliquesQuery = Clique.table
        .filter(_.name.toLowerCase like searchTerm)
        .take(limit)
        .result
      
      for {
        users <- database.run(usersQuery)
        clusters <- database.run(clustersQuery)
        cliques <- database.run(cliquesQuery)
        
        // Get follower counts for users
        userResults <- Future.sequence(users.map { user =>
          val followersQuery = UserUserAssociation.table.filter(assoc =>
            assoc.obj === user.id && assoc.follows === true
          ).length.result
          val followingQuery = UserUserAssociation.table.filter(assoc =>
            assoc.subj === user.id && assoc.follows === true
          ).length.result
          for {
            followers <- database.run(followersQuery).recover(_ => 0)
            following <- database.run(followingQuery).recover(_ => 0)
          } yield SearchResult(
            user.id.value,
            user.username,
            "user",
            Temp.imageURL,
            s"$followers followers · $following following"
          )
        })
        
        // Get follower counts for clusters
        clusterResults <- Future.sequence(clusters.map { cluster =>
          val followersQuery = UserClusterAssociation.table.filter(assoc =>
            assoc.obj === cluster.id && assoc.isFollowing === true
          ).length.result
          val postsQuery = Post.table.filter(_.cluster === cluster.id).length.result
          for {
            followers <- database.run(followersQuery).recover(_ => 0)
            posts <- database.run(postsQuery).recover(_ => 0)
          } yield SearchResult(
            cluster.id.value,
            cluster.title,
            "cluster",
            Temp.imageURL,
            s"$followers followers · $posts posts"
          )
        })
        
        // Get member/follower counts for cliques
        cliqueResults <- Future.sequence(cliques.map { clique =>
          val membersQuery = UserCliqueAssociation.table.filter(assoc =>
            assoc.obj === clique.id && (assoc.memberRole === CliqueMemberRole.Member || 
                                       assoc.memberRole === CliqueMemberRole.Moderator || 
                                       assoc.memberRole === CliqueMemberRole.Admin)
          ).length.result
          val followersQuery = UserCliqueAssociation.table.filter(assoc =>
            assoc.obj === clique.id && assoc.memberRole === CliqueMemberRole.Following
          ).length.result
          for {
            members <- database.run(membersQuery).recover(_ => 0)
            followers <- database.run(followersQuery).recover(_ => 0)
          } yield SearchResult(
            clique.id.value,
            clique.name,
            "clique",
            Temp.imageURL,
            s"$members members · $followers followers"
          )
        })
      } yield {
        Ok(Json.obj(
          "users" -> Json.toJson(userResults),
          "clusters" -> Json.toJson(clusterResults),
          "cliques" -> Json.toJson(cliqueResults)
        ))
      }
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Search failed", "details" -> "An unexpected error occurred"))
    }(databaseEc)
  }

  // =========================================================================
  // Profile Update
  // =========================================================================
  
  case class UpdateProfileRequest(
                                   displayName: Option[String],
                                   biography: Option[String],
                                   image: Option[String]
                                 )

  implicit val updateProfileRequestReads: Reads[UpdateProfileRequest] = Json.reads[UpdateProfileRequest]

  def updateProfile(): Action[JsValue] = authorizedAction.async(parse.json) { implicit auth_request =>
    auth_request.body.validate[UpdateProfileRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> JsError.toJson(errors)))),
      profileReq => {
        // Get the current user
        val userQuery = User.table.filter(_.id === auth_request.user).result.headOption
        
        database.run(userQuery).flatMap {
          case Some(user) =>
            // Build update query based on provided fields
            val updates = Seq(
              profileReq.displayName.map(name => sqlu"UPDATE user SET name = $name WHERE id = ${auth_request.user.value}"),
              profileReq.biography.map(bio => sqlu"UPDATE user SET biography = $bio WHERE id = ${auth_request.user.value}"),
              profileReq.image.map(img => sqlu"UPDATE user SET image = $img WHERE id = ${auth_request.user.value}")
            ).flatten
            
            if (updates.isEmpty) {
              Future.successful(BadRequest(Json.obj("error" -> "No fields to update")))
            } else {
              // Execute all updates
              Future.sequence(updates.map(database.run)).flatMap { _ =>
                // Return updated user profile
                val updatedUserQuery = User.table.filter(_.id === auth_request.user).result.head
                database.run(updatedUserQuery).flatMap { updatedUser =>
                  updatedUser.profile.map { profile =>
                    Ok(Json.obj(
                      "message" -> "Profile updated successfully",
                      "profile" -> Json.toJson(profile)
                    ))
                  }
                }
              }
            }
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "User not found")))
        }.recover {
          case ex: Exception =>
            ex.printStackTrace()
            InternalServerError(Json.obj("error" -> "Failed to update profile", "details" -> "An unexpected error occurred"))
        }(databaseEc)
      }
    )
  }

  // =========================================================================
  // Settings Update
  // =========================================================================
  
  case class UpdateSettingsRequest(
                                    privacy: Option[PrivacySettings],
                                    notifications: Option[NotificationSettings],
                                    ui: Option[UISettings],
                                    player: Option[PlayerSettings],
                                    location: Option[LocationData]
                                  )

  implicit val updateSettingsRequestReads: Reads[UpdateSettingsRequest] = Json.reads[UpdateSettingsRequest]

  def getSettings(): Action[AnyContent] = authorizedAction.async { implicit auth_request =>
    val settingsQuery = UserSettings.table.filter(_.userId === auth_request.user).result.headOption
    
    database.run(settingsQuery).map {
      case Some(settings) =>
        val privacySettings = Json.parse(settings.privacyJson).as[PrivacySettings]
        val notificationSettings = Json.parse(settings.notificationsJson).as[NotificationSettings]
        val uiSettings = Json.parse(settings.uiJson).as[UISettings]
        val playerSettings = Json.parse(settings.playerJson).as[PlayerSettings]
        val locationData = settings.locationJson.map(json => Json.parse(json).as[LocationData])
        
        Ok(Json.toJson(UserSettingsBlob(
          auth_request.user.value,
          privacySettings,
          notificationSettings,
          uiSettings,
          playerSettings,
          locationData
        )))
      case None =>
        // Return default settings
        Ok(Json.toJson(UserSettingsBlob(
          auth_request.user.value,
          PrivacySettings(),
          NotificationSettings(),
          UISettings(),
          PlayerSettings(),
          None
        )))
    }.recover {
      case ex: Exception =>
        ex.printStackTrace()
        InternalServerError(Json.obj("error" -> "Failed to get settings", "details" -> "An unexpected error occurred"))
    }(databaseEc)
  }

  def updateSettings(): Action[JsValue] = authorizedAction.async(parse.json) { implicit auth_request =>
    auth_request.body.validate[UpdateSettingsRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid request format", "details" -> JsError.toJson(errors)))),
      settingsReq => {
        val now = Timestamp.valueOf(LocalDateTime.now())
        
        // Check if settings exist for this user
        val existingQuery = UserSettings.table.filter(_.userId === auth_request.user).result.headOption
        
        database.run(existingQuery).flatMap {
          case Some(existing) =>
            // Update existing settings
            val privacyJson = settingsReq.privacy.map(p => Json.toJson(p).toString()).getOrElse(existing.privacyJson)
            val notificationsJson = settingsReq.notifications.map(n => Json.toJson(n).toString()).getOrElse(existing.notificationsJson)
            val uiJson = settingsReq.ui.map(u => Json.toJson(u).toString()).getOrElse(existing.uiJson)
            val playerJson = settingsReq.player.map(p => Json.toJson(p).toString()).getOrElse(existing.playerJson)
            val locationJson = settingsReq.location.map(l => Some(Json.toJson(l).toString())).getOrElse(existing.locationJson)
            
            val updateQuery = UserSettings.table
              .filter(_.userId === auth_request.user)
              .map(s => (s.privacyJson, s.notificationsJson, s.uiJson, s.playerJson, s.locationJson, s.updatedAt))
              .update((privacyJson, notificationsJson, uiJson, playerJson, locationJson, now))
            
            database.run(updateQuery).map { _ =>
              Ok(Json.obj(
                "message" -> "Settings updated successfully",
                "settings" -> Json.toJson(UserSettingsBlob(
                  auth_request.user.value,
                  settingsReq.privacy.getOrElse(Json.parse(existing.privacyJson).as[PrivacySettings]),
                  settingsReq.notifications.getOrElse(Json.parse(existing.notificationsJson).as[NotificationSettings]),
                  settingsReq.ui.getOrElse(Json.parse(existing.uiJson).as[UISettings]),
                  settingsReq.player.getOrElse(Json.parse(existing.playerJson).as[PlayerSettings]),
                  settingsReq.location.orElse(existing.locationJson.map(json => Json.parse(json).as[LocationData]))
                ))
              ))
            }
          case None =>
            // Create new settings record
            val privacySettings = settingsReq.privacy.getOrElse(PrivacySettings())
            val notificationSettings = settingsReq.notifications.getOrElse(NotificationSettings())
            val uiSettings = settingsReq.ui.getOrElse(UISettings())
            val playerSettings = settingsReq.player.getOrElse(PlayerSettings())
            val locationData = settingsReq.location
            
            val newSettings = UserSettingsConnection(
              Identity[UserSettingsEntity](0),
              auth_request.user,
              Json.toJson(privacySettings).toString(),
              Json.toJson(notificationSettings).toString(),
              Json.toJson(uiSettings).toString(),
              Json.toJson(playerSettings).toString(),
              locationData.map(l => Json.toJson(l).toString()),
              now
            )
            
            database.run(UserSettings.table += newSettings).map { _ =>
              Created(Json.obj(
                "message" -> "Settings created successfully",
                "settings" -> Json.toJson(UserSettingsBlob(
                  auth_request.user.value,
                  privacySettings,
                  notificationSettings,
                  uiSettings,
                  playerSettings,
                  locationData
                ))
              ))
            }
        }.recover {
          case ex: Exception =>
            ex.printStackTrace()
            InternalServerError(Json.obj("error" -> "Failed to update settings", "details" -> "An unexpected error occurred"))
        }(databaseEc)
      }
    )
  }
}