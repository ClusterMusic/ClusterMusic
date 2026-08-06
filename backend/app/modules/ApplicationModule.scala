package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.{Configuration, Environment}
import auth.{ClusterGuardian, Guardian}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import slick.jdbc.MySQLProfile.api._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import model._

import java.sql.Timestamp
import java.util.concurrent.{Executors, ForkJoinPool}
import javax.inject.Named

class ApplicationModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  
  override def configure(): Unit = {
    
    
    bind(classOf[Guardian]).toInstance(ClusterGuardian)
  }

  @Provides
  @Singleton
  @Named("database")
  def provideDatabaseExecutionContext(configuration: Configuration): ExecutionContext = {
    val dbThreads = configuration.getOptional[Int]("app.database.thread-pool.size").getOrElse(20)
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(dbThreads, (r: Runnable) => {
        val thread = new Thread(r, "database-thread")
        thread.setDaemon(true)
        thread
      })
    )
  }

  @Provides
  @Singleton
  @Named("blocking")
  def provideBlockingExecutionContext(configuration: Configuration): ExecutionContext = {
    val blockingThreads = configuration.getOptional[Int]("app.blocking.thread-pool.size").getOrElse(32)
    ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool((r: Runnable) => {
        val thread = new Thread(r, "blocking-io-thread")
        thread.setDaemon(true)
        thread
      })
    )
  }
  

  @Provides
  @Singleton
  @Named("cpu-intensive")
  def provideCpuIntensiveExecutionContext(configuration: Configuration): ExecutionContext = {
    val cpuThreads = configuration.getOptional[Int]("app.cpu-intensive.thread-pool.size")
      .getOrElse(Runtime.getRuntime.availableProcessors())
    ExecutionContext.fromExecutor(
      new ForkJoinPool(cpuThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)
    )
  }
  
  

  @Provides
  @Singleton
  def provideMongoDatabase(configuration: Configuration): MongoDatabase = {
    val mongoUri = configuration.get[String]("mongodb.uri")
    val mongoClient = MongoClient(mongoUri)
    mongoClient.getDatabase("chat_db")
  }
  
  @Provides
  @Singleton
  def provideSlickDatabase(
    dbConfigProvider: DatabaseConfigProvider, 
    @Named("database") dbEc: ExecutionContext
  ): Database = {
    val dbConfig = dbConfigProvider.get[MySQLProfile]
    val db = dbConfig.db.asInstanceOf[Database]

    val schemas = Seq(
      User.table.schema,
      Community.table.schema,
      Cluster.table.schema,
      Clique.table.schema,
      Post.table.schema,
      Comment.table.schema,
      Song.table.schema,
      SpotifySong.table.schema,
      AppleMusicSong.table.schema,
      UserUserAssociation.table.schema,
      UserPostAssociation.table.schema,
      UserCommentAssociation.table.schema,
      UserClusterAssociation.table.schema,
      UserCliqueAssociation.table.schema,
      UserAchievement.table.schema,
      UserAchievementAssociation.table.schema,
      CliqueAchievement.table.schema,
      CliqueAchievementAssociation.table.schema,
      UserSettings.table.schema
    ).reduce(_ ++ _)

    
    try {
      Await.result(db.run(schemas.create), 30.seconds)
      println(s"[SchemaCreation] Successfully created database schema")
    } catch {
      case ex: Exception if ex.getMessage != null && (
        ex.getMessage.contains("Duplicate key name") || 
        ex.getMessage.contains("already exists") ||
        ex.getMessage.contains("Table") && ex.getMessage.contains("already exists")
      ) =>
        println(s"[SchemaCreation] Database schema already exists, continuing startup")
      case ex: Exception =>
        println(s"[SchemaCreation] Warning: ${ex.getMessage}")
        println(s"[SchemaCreation] Attempting to continue with existing schema...")
    }

    
    implicit val implicitDbEc: ExecutionContext = dbEc
    val seedOnStartup = configuration.getOptional[Boolean]("app.seedOnStartup").getOrElse(true)
    if (seedOnStartup) {
      try {
        val seedCommunity: DBIO[Unit] = for {
          _ <- sqlu"SET SESSION sql_mode = CONCAT(@@sql_mode, ',NO_AUTO_VALUE_ON_ZERO')"
          exists <- Community.table.filter(_.id === Identity[Community](0)).exists.result
          _ <- if (!exists) {
            val now = java.time.LocalDateTime.now()
            Community.table += CommunityConnection(Identity[Community](0), "none", "Default Community", "{}", Timestamp.valueOf(now))
          } else DBIO.successful(())
        } yield ()

        Await.result(db.run(seedCommunity.transactionally), 10.seconds)
        println("[Seed] Ensured Community(id=0) exists")
      } catch {
        case ex: Exception =>
          println(s"[Seed] Failed to ensure Community(id=0): ${ex.getMessage}")
      }

      // Seed dummy content
      try {
        val now = Timestamp.valueOf(java.time.LocalDateTime.now())
        
        val seedDummyContent: DBIO[Unit] = for {
          // Check if dummy data already exists
          userExists <- User.table.filter(_.username === "demo_user").exists.result
          _ <- if (!userExists) {
            for {
              // Create additional communities
              _ <- Community.table ++= Seq(
                CommunityConnection(Identity[Community](1), "nyc.png", "New York City", "{}", now),
                CommunityConnection(Identity[Community](2), "la.png", "Los Angeles", "{}", now),
                CommunityConnection(Identity[Community](3), "chicago.png", "Chicago", "{}", now)
              )
              
              // Create users
              _ <- User.table ++= Seq(
                UserConnection(Identity[User](1), "demo_user", "password123", "Demo User", "", "Music lover and playlist curator", Identity[Community](1), now, 0.5, 100.0),
                UserConnection(Identity[User](2), "indie_fan", "password123", "Indie Fan", "", "Discovering new sounds daily", Identity[Community](1), now, 0.4, 80.0),
                UserConnection(Identity[User](3), "hip_hop_head", "password123", "Hip Hop Head", "", "90s hip hop enthusiast", Identity[Community](2), now, 0.6, 120.0),
                UserConnection(Identity[User](4), "rock_lover", "password123", "Rock Lover", "", "Classic rock forever", Identity[Community](2), now, 0.3, 60.0),
                UserConnection(Identity[User](5), "pop_princess", "password123", "Pop Princess", "", "Top 40 hits all day", Identity[Community](3), now, 0.7, 150.0)
              )
              
              // Create clusters (genres/categories)
              _ <- Cluster.table ++= Seq(
                ClusterConnection(Identity[Cluster](1), "Indie", None, Identity[Community](1), now),
                ClusterConnection(Identity[Cluster](2), "Hip Hop", None, Identity[Community](1), now),
                ClusterConnection(Identity[Cluster](3), "Rock", None, Identity[Community](1), now),
                ClusterConnection(Identity[Cluster](4), "Pop", None, Identity[Community](1), now),
                ClusterConnection(Identity[Cluster](5), "Electronic", None, Identity[Community](1), now),
                ClusterConnection(Identity[Cluster](6), "Jazz", None, Identity[Community](2), now),
                ClusterConnection(Identity[Cluster](7), "R&B", None, Identity[Community](2), now),
                ClusterConnection(Identity[Cluster](8), "Country", None, Identity[Community](3), now)
              )
              
              // Create cliques (groups)
              _ <- Clique.table ++= Seq(
                CliqueConnection(Identity[Clique](1), "Chill Vibes", "A place for relaxing music", 1, now, 50.0, 0.5),
                CliqueConnection(Identity[Clique](2), "Workout Beats", "High energy tracks for the gym", 2, now, 75.0, 0.6),
                CliqueConnection(Identity[Clique](3), "Late Night Jams", "Perfect for those late nights", 3, now, 60.0, 0.4),
                CliqueConnection(Identity[Clique](4), "Throwback Thursday", "Classic hits from the past", 4, now, 90.0, 0.7),
                CliqueConnection(Identity[Clique](5), "New Releases", "Fresh tracks dropping weekly", 5, now, 100.0, 0.8)
              )
              
              // Create songs
              _ <- Song.table ++= Seq(
                SongConnection(Identity[Song](1), "Blinding Lights", None, None, now, 95),
                SongConnection(Identity[Song](2), "Levitating", None, None, now, 88),
                SongConnection(Identity[Song](3), "Good 4 U", None, None, now, 92),
                SongConnection(Identity[Song](4), "Stay", None, None, now, 90),
                SongConnection(Identity[Song](5), "Peaches", None, None, now, 85),
                SongConnection(Identity[Song](6), "Montero", None, None, now, 87),
                SongConnection(Identity[Song](7), "Kiss Me More", None, None, now, 83),
                SongConnection(Identity[Song](8), "Deja Vu", None, None, now, 80),
                SongConnection(Identity[Song](9), "Drivers License", None, None, now, 94),
                SongConnection(Identity[Song](10), "Save Your Tears", None, None, now, 89)
              )
              
              // Create comments (captions for posts)
              _ <- Comment.table ++= Seq(
                CommentConnection(Identity[Comment](1), "This song hits different at night 🌙", Identity[User](1), None, now),
                CommentConnection(Identity[Comment](2), "Can't stop listening to this!", Identity[User](2), None, now),
                CommentConnection(Identity[Comment](3), "Summer vibes all year round ☀️", Identity[User](3), None, now),
                CommentConnection(Identity[Comment](4), "The beat drop is insane", Identity[User](4), None, now),
                CommentConnection(Identity[Comment](5), "Perfect for my morning commute", Identity[User](5), None, now),
                CommentConnection(Identity[Comment](6), "Throwback to simpler times", Identity[User](1), None, now),
                CommentConnection(Identity[Comment](7), "This artist never disappoints", Identity[User](2), None, now),
                CommentConnection(Identity[Comment](8), "Currently on repeat 🔁", Identity[User](3), None, now),
                CommentConnection(Identity[Comment](9), "The lyrics really speak to me", Identity[User](4), None, now),
                CommentConnection(Identity[Comment](10), "Best song of the year!", Identity[User](5), None, now)
              )
              
              // Create posts
              _ <- Post.table ++= Seq(
                PostConnection(Identity[Post](1), Identity[Comment](1), Identity[Song](1), Identity[User](1), Identity[Clique](1), Identity[Cluster](4), now, 10, 50, 0.8, 200.0),
                PostConnection(Identity[Post](2), Identity[Comment](2), Identity[Song](2), Identity[User](2), Identity[Clique](2), Identity[Cluster](4), now, 8, 40, 0.7, 180.0),
                PostConnection(Identity[Post](3), Identity[Comment](3), Identity[Song](3), Identity[User](3), Identity[Clique](3), Identity[Cluster](3), now, 15, 60, 0.9, 250.0),
                PostConnection(Identity[Post](4), Identity[Comment](4), Identity[Song](4), Identity[User](4), Identity[Clique](4), Identity[Cluster](4), now, 5, 30, 0.5, 120.0),
                PostConnection(Identity[Post](5), Identity[Comment](5), Identity[Song](5), Identity[User](5), Identity[Clique](5), Identity[Cluster](4), now, 12, 55, 0.75, 190.0),
                PostConnection(Identity[Post](6), Identity[Comment](6), Identity[Song](6), Identity[User](1), Identity[Clique](1), Identity[Cluster](2), now, 20, 80, 0.85, 300.0),
                PostConnection(Identity[Post](7), Identity[Comment](7), Identity[Song](7), Identity[User](2), Identity[Clique](2), Identity[Cluster](7), now, 7, 35, 0.6, 150.0),
                PostConnection(Identity[Post](8), Identity[Comment](8), Identity[Song](8), Identity[User](3), Identity[Clique](3), Identity[Cluster](4), now, 18, 70, 0.82, 220.0),
                PostConnection(Identity[Post](9), Identity[Comment](9), Identity[Song](9), Identity[User](4), Identity[Clique](4), Identity[Cluster](4), now, 25, 100, 0.95, 350.0),
                PostConnection(Identity[Post](10), Identity[Comment](10), Identity[Song](10), Identity[User](5), Identity[Clique](5), Identity[Cluster](4), now, 22, 90, 0.88, 280.0)
              )
              
              // Create user-user associations (follows)
              _ <- UserUserAssociation.table ++= Seq(
                UserUserAssociationConnection(Identity[User](1), Identity[User](2), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](1), Identity[User](3), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](2), Identity[User](1), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](2), Identity[User](5), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](3), Identity[User](4), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](4), Identity[User](1), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](5), Identity[User](1), follows = true, Some(now), now),
                UserUserAssociationConnection(Identity[User](5), Identity[User](2), follows = true, Some(now), now)
              )
              
              // Create user-clique associations (memberships)
              _ <- UserCliqueAssociation.table ++= Seq(
                UserCliqueAssociationConnection(Identity[User](1), Identity[Clique](1), CliqueMemberRole.Admin, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](1), Identity[Clique](2), CliqueMemberRole.Member, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](2), Identity[Clique](1), CliqueMemberRole.Member, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](2), Identity[Clique](3), CliqueMemberRole.Admin, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](3), Identity[Clique](2), CliqueMemberRole.Member, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](3), Identity[Clique](4), CliqueMemberRole.Admin, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](4), Identity[Clique](3), CliqueMemberRole.Member, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](4), Identity[Clique](5), CliqueMemberRole.Admin, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](5), Identity[Clique](4), CliqueMemberRole.Member, Some(now), Some(now), now),
                UserCliqueAssociationConnection(Identity[User](5), Identity[Clique](5), CliqueMemberRole.Member, Some(now), Some(now), now)
              )
              
              // Create user-cluster associations (follows)
              _ <- UserClusterAssociation.table ++= Seq(
                UserClusterAssociationConnection(Identity[User](1), Identity[Cluster](1), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](1), Identity[Cluster](4), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](2), Identity[Cluster](1), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](2), Identity[Cluster](5), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](3), Identity[Cluster](2), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](3), Identity[Cluster](7), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](4), Identity[Cluster](3), isFollowing = true, Some(now), Some(now), now),
                UserClusterAssociationConnection(Identity[User](5), Identity[Cluster](4), isFollowing = true, Some(now), Some(now), now)
              )
            } yield ()
          } else DBIO.successful(())
        } yield ()

        Await.result(db.run(seedDummyContent.transactionally), 30.seconds)
        println("[Seed] Dummy content seeded successfully")
      } catch {
        case ex: Exception =>
          println(s"[Seed] Dummy content seeding skipped or failed: ${ex.getMessage}")
      }
    }

    db
  }
} 