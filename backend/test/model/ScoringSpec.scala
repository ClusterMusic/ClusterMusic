package model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalacheck.{Gen, Shrink}

class ScoringSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  implicit private def noShrink[A]: Shrink[A] = Shrink.shrinkAny

  private val count: Gen[Int] = Gen.choose(0, 100000)
  private val positiveCount: Gen[Int] = Gen.choose(1, 100000)
  private val ageInDays: Gen[Double] = Gen.choose(0.0, 3650.0)
  private val score: Gen[Double] = Gen.choose(0.0, 1000.0)
  private val scores: Gen[List[Double]] = Gen.nonEmptyListOf(score)

  private def beFinite(d: Double): Unit = {
    d.isNaN shouldBe false
    d.isInfinite shouldBe false
  }

  "postRank" should {

    "stay finite and non-negative for any non-negative engagement" in {
      forAll(count, count, count, count, ageInDays) {
        (views: Int, rewatches: Int, likes: Int, comments: Int, days: Double) =>
          val rank = Scoring.postRank(views, rewatches, likes, comments, days)
          beFinite(rank)
          rank should be >= 0.0
      }
    }

    "never decrease when a post gains a like" in {
      forAll(count, count, count, count, ageInDays) {
        (views: Int, rewatches: Int, likes: Int, comments: Int, days: Double) =>
          val before = Scoring.postRank(views, rewatches, likes, comments, days)
          val after = Scoring.postRank(views, rewatches, likes + 1, comments, days)
          after should be >= before
      }
    }

    "never decrease when a post gains a unique view" in {
      forAll(count, count, count, count, ageInDays) {
        (views: Int, rewatches: Int, likes: Int, comments: Int, days: Double) =>
          val before = Scoring.postRank(views, rewatches, likes, comments, days)
          val after = Scoring.postRank(views + 1, rewatches, likes, comments, days)
          after should be >= before
      }
    }

    "weight a comment more heavily than a like" in {
      forAll(positiveCount, count, positiveCount) {
        (views: Int, rewatches: Int, n: Int) =>
          val fromLikes = Scoring.postRank(views, rewatches, n, 0, 1.0)
          val fromComments = Scoring.postRank(views, rewatches, 0, n, 1.0)
          fromComments should be > fromLikes
      }
    }

    "decay as a post gets older" in {
      forAll(positiveCount, count, count, count, ageInDays) {
        (views: Int, rewatches: Int, likes: Int, comments: Int, days: Double) =>
          val younger = Scoring.postRank(views, rewatches, likes, comments, days)
          val older = Scoring.postRank(views, rewatches, likes, comments, days + 1.0)
          older should be < younger
      }
    }
  }

  "cliqueScore" should {

    "stay finite for any clique with at least one post" in {
      forAll(scores, count, positiveCount) {
        (postScores: List[Double], followers: Int, cliqueSize: Int) =>
          forAll(Gen.choose(0, cliqueSize)) { (activeSize: Int) =>
            beFinite(Scoring.cliqueScore(postScores, followers, cliqueSize, activeSize))
          }
      }
    }

    "be positive when a clique has positive post scores" in {
      forAll(Gen.nonEmptyListOf(Gen.choose(1.0, 1000.0)), positiveCount, positiveCount) {
        (postScores: List[Double], followers: Int, cliqueSize: Int) =>
          Scoring.cliqueScore(postScores, followers, cliqueSize, cliqueSize) should be > 0.0
      }
    }

    "not produce NaN for a clique that has no posts yet" in {
      beFinite(Scoring.cliqueScore(Seq.empty, followers = 10, cliqueSize = 5, activeSize = 5))
    }

    "not zero out a clique that has fewer posts than members" in {
      val score = Scoring.cliqueScore(
        postScores = Seq(500.0, 400.0, 300.0),
        followers = 50,
        cliqueSize = 10,
        activeSize = 10
      )
      score should be > 0.0
    }
  }

  "cliqueRank" should {
    "decay as the clique's activity gets older" in {
      forAll(Gen.choose(0.1, 1000.0), ageInDays) { (base: Double, days: Double) =>
        Scoring.cliqueRank(base, days + 1.0) should be < Scoring.cliqueRank(base, days)
      }
    }
  }

  "userScore" should {

    "stay finite and non-negative for any follower/following counts" in {
      forAll(scores, count, count, count) {
        (postScores: List[Double], followers: Int, following: Int, cliques: Int) =>
          val s = Scoring.userScore(postScores, followers, following, cliques)
          beFinite(s)
          s should be >= 0.0
      }
    }

    "be zero for a user who has never posted" in {
      Scoring.userScore(Seq.empty, followers = 100, following = 50, cliqueCount = 3) shouldBe 0.0
    }

    "never decrease when a user gains a follower" in {
      forAll(scores, count, count, count) {
        (postScores: List[Double], followers: Int, following: Int, cliques: Int) =>
          val before = Scoring.userScore(postScores, followers, following, cliques)
          val after = Scoring.userScore(postScores, followers + 1, following, cliques)
          after should be >= before
      }
    }
  }

  "userRank" should {

    "stay finite and non-negative" in {
      forAll(scores, count, count, ageInDays) {
        (postScores: List[Double], followers: Int, following: Int, days: Double) =>
          val r = Scoring.userRank(postScores, followers, following, days)
          beFinite(r)
          r should be >= 0.0
      }
    }

    "be zero for a user who has never posted" in {
      Scoring.userRank(Seq.empty, followers = 100, following = 50, timeDays = 1.0) shouldBe 0.0
    }
  }

  "postScore" should {

    "stay finite for any positive community bias" in {
      val flags = for { promoted <- Seq(true, false); global <- Seq(true, false) } yield (promoted, global)
      forAll(Gen.choose(0.01, 100.0), count, count, count, count, count) {
        (bias: Double, views: Int, rewatches: Int, likes: Int, comments: Int, songs: Int) =>
          flags.foreach { case (promoted, global) =>
            withClue(s"promoted=$promoted global=$global: ") {
              beFinite(Scoring.postScore(promoted, bias, global, views, rewatches, likes, comments, songs))
            }
          }
      }
    }

    "shrink in magnitude as a post crams in more songs" in {
      forAll(Gen.choose(2.0, 100.0), positiveCount, count, count, count, count) {
        (bias: Double, views: Int, rewatches: Int, likes: Int, comments: Int, songs: Int) =>
          val fewer = Scoring.postScore(false, bias, false, views, rewatches, likes, comments, songs)
          val more = Scoring.postScore(false, bias, false, views, rewatches, likes, comments, songs + 1)
          more.abs should be <= fewer.abs
      }
    }

    "reward a post with real engagement" in {
      for {
        bias <- Seq(Scoring.NO_COMMUNITY_BIAS, 1.0) 
        promoted <- Seq(true, false)
        global <- Seq(true, false)
      } withClue(s"bias=$bias promoted=$promoted global=$global: ") {
        Scoring.postScore(promoted, bias, global,
          uniqueViews = 500, rewatchViews = 100, N_likes = 250, N_Comments = 40,
          N_Songs = 1) should be > 0.0
      }
    }

    "treat an absent community signal as neutral rather than disqualifying" in {
      val neutral = Scoring.postScore(false, Scoring.NO_COMMUNITY_BIAS, false, 500, 100, 250, 40, 1)
      val average = Scoring.postScore(false, 1.0, false, 500, 100, 250, 40, 1)
      neutral shouldBe average
    }

    "rank a post above an identical post with less engagement" in {
      forAll(Gen.choose(0.5, 64.0), positiveCount, count, count) {
        (bias: Double, views: Int, likes: Int, comments: Int) =>
          val quieter = Scoring.postScore(true, bias, false, views, 0, likes, comments, 1)
          val louder = Scoring.postScore(true, bias, false, views, 0, likes + 1, comments, 1)
          louder should be >= quieter
      }
    }

    "damp but never zero a post from an under-performing community" in {
      val damped = Scoring.postScore(false, 0.25, false, 500, 100, 250, 40, 1)
      val neutral = Scoring.postScore(false, 1.0, false, 500, 100, 250, 40, 1)
      damped should be > 0.0
      damped should be < neutral
    }

    "boost a post from an over-performing community" in {
      val boosted = Scoring.postScore(false, 8.0, false, 500, 100, 250, 40, 1)
      val neutral = Scoring.postScore(false, 1.0, false, 500, 100, 250, 40, 1)
      boosted should be > neutral
    }

    "stay finite for a zero or negative community bias" in {
      Seq(0.0, -1.0, -5.0).foreach { bias =>
        withClue(s"bias=$bias: ") {
          beFinite(Scoring.postScore(true, bias, true, 500, 100, 250, 40, 1))
        }
      }
    }

    "separate posts by engagement once a community bias above 1 is supplied" in {
      val quiet = Scoring.postScore(false, 4.0, false, 10, 0, 1, 0, 1)
      val busy = Scoring.postScore(false, 4.0, false, 5000, 900, 2000, 300, 1)
      busy should be > quiet
    }
  }
}
