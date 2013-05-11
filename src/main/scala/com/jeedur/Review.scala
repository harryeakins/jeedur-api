package com.jeedur

import org.joda.time.{Duration, Interval, DateTime}

object Difficulty extends Enumeration {
  type Difficulty = Value
  val EASY = Value("EASY")
  val FAIL = Value("FAIL")
}

import Difficulty._

object Review {
  val REVIEW_BACKOFF_FACTOR = 3
  val INITIAL_BACKOFF = Duration.standardHours(3)

  def getNextStudyTime(reviews: List[Review]): DateTime = {
    val sortedReviews = reviews.sortBy(_.reviewDate.getMillis).reverse
    val lastReviewTime = if (!sortedReviews.isEmpty) sortedReviews.head.reviewDate else new DateTime(0)

    val (correctReviews, _) = sortedReviews.span(_.difficulty == EASY)

    correctReviews match {
      case Nil => lastReviewTime

      case Review(reviewDate, _, _, EASY) :: Nil => reviewDate.plus(INITIAL_BACKOFF)

      case latestSetOfCorrectReviews =>
        val durations = (latestSetOfCorrectReviews.tail zip latestSetOfCorrectReviews.init).map {
          case (review1, review2) => new Interval(review1.reviewDate, review2.reviewDate).toDuration
        }.sortBy(i => i.getMillis).reverse

        val longestDuration = durations.head

        val nextDuration = Duration.millis(longestDuration.getMillis * REVIEW_BACKOFF_FACTOR)
        if (nextDuration.getMillis < INITIAL_BACKOFF.getMillis) {
          lastReviewTime.plus(INITIAL_BACKOFF)
        } else {
          lastReviewTime.plus(nextDuration)
        }
    }
  }
}

case class Review(reviewDate: DateTime, timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) {
  def this(timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) = this(DateTime.now(), timeOnFront, timeOnBack, difficulty)
}