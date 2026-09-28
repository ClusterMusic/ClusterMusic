package model

object Scoring {
  val ALPHA: Double = 0.05
  val BETA: Double = 3.5
  val DELTA: Double = 14
  val THETA: Double = 1.2
  val EPSILON: Double = 1.05
  val PHI: Double = 0.18
  val ZETA: Double = 0.5
  val G: Double = 1.8
  val P: Double = 1.2
  val M0: Double = 10
  val MIN_BIAS: Double = 0.1

  val NO_COMMUNITY_BIAS: Double = -1.0


  private def cliquePenalty(cliqueSize: Int, activeSize: Int): Double = {
    val ratio = cliqueSize.toDouble / M0
    val basePenalty = math.max(1.0, math.pow(ratio, THETA))
    val activityFactor = 1 + EPSILON * math.max(0, cliqueSize - activeSize)
    basePenalty * activityFactor
  }

  private def followingFactor(followers: Int, cliqueSize: Int): Double = {
    1 + EPSILON * math.log(1 + followers.toDouble / cliqueSize)
  }

  private def postFactor(postScores: Seq[Double], cliqueSize: Int): Double = {
    if (postScores.isEmpty || cliqueSize <= 0) 0.0
    else {
      val meanRootScore = postScores.map(score => math.sqrt(score)).sum / postScores.size
      val postsPerMember = postScores.size.toDouble / cliqueSize.toDouble
      meanRootScore * math.log(1 + postsPerMember)
    }
  }

  private def freshness(timeDays: Double): Double = {
    DELTA / (timeDays + 1)
  }

  private def engagement(uniqueViews: Int,
                         rewatchViews: Int,
                         N_likes: Int,
                         N_Comments: Int): Double = {
    attention(uniqueViews, rewatchViews) * feedback(N_likes, N_Comments)
  }

  private def attention(uniqueViews: Int, rewatchedViews: Int): Double = {
    math.log(1 + uniqueViews) + ALPHA * math.log(1 + rewatchedViews)
  }

  private def feedback(N_Likes: Int, N_Comments: Int): Double = {
    math.log(1 + N_Likes + BETA * N_Comments)
  }

  private def postBias(promotedClique: Boolean, communityBias: Double = NO_COMMUNITY_BIAS, globalBias: Boolean): Double = {
    val communityBoost =
      if (communityBias <= 0.0) 0.0
      else math.log(communityBias) / math.log(2)

    val amplified = if (globalBias) communityBoost * G else communityBoost
    val boost = if (promotedClique) P * amplified else amplified
    
    math.max(MIN_BIAS, 1.0 + boost)
  }

  private def uniquenessPenalty(N_Songs: Int): Double = {
    1 + math.log(1 + N_Songs)
  }

  def cliqueScore(postScores: Seq[Double],
                  followers: Int,
                  cliqueSize: Int,
                  activeSize: Int): Double = {
    postFactor(postScores, cliqueSize) * followingFactor(followers, cliqueSize) / cliquePenalty(cliqueSize, activeSize)
  }

  def postRank(uniqueViews: Int,
               rewatchViews: Int,
               N_likes: Int,
               N_Comments: Int,
               timeDays: Double): Double = {
    engagement(uniqueViews, rewatchViews, N_likes, N_Comments) * freshness(timeDays)
  }


  def postScore(promotedClique: Boolean,
                communityBias: Double = -1.0,
                globalBias: Boolean,
                uniqueViews: Int,
                rewatchViews: Int,
                N_likes: Int,
                N_Comments: Int,
                N_Songs: Int): Double = {
    postBias(promotedClique, communityBias, globalBias) *
      engagement(uniqueViews, rewatchViews, N_likes, N_Comments) /
      uniquenessPenalty(N_Songs)
  }

  def userRank(postScores: Seq[Double],
               followers: Int,
               following: Int,
               timeDays: Double): Double = {
    val avgPostScore = if (postScores.nonEmpty) postScores.sum / postScores.size else 0.0
    val socialFactor = math.log(1 + followers) * math.log(1 + following) / (following + 1)
    val activityFactor = math.log(1 + postScores.size)
    
    (avgPostScore * socialFactor * activityFactor) * freshness(timeDays)
  }

  def cliqueRank(cliqueScore: Double,
                 timeDays: Double): Double = {
    cliqueScore * freshness(timeDays)
  }
  def userScore(postScores: Seq[Double],
                followers: Int,
                following: Int,
                cliqueCount: Int): Double = {
    val avgPostScore = if (postScores.nonEmpty) postScores.sum / postScores.size else 0.0
    val socialFactor = math.log(1 + followers) / math.log(1 + following + 1)
    val activityFactor = math.log(1 + postScores.size)
    val communityFactor = math.log(1 + cliqueCount)
    
    avgPostScore * socialFactor * activityFactor * communityFactor
  }
}