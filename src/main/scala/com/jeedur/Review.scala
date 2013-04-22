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
    val sortedReviews = reviews.sortBy(_.reviewDate.getMillis)
    val (correctReviews, _) = sortedReviews.span(_.difficulty == EASY)
    correctReviews match {
      case Nil => DateTime.now()
      case Review(reviewDate, _, _, EASY) :: Nil => reviewDate.plus(INITIAL_BACKOFF)
      case correctReviews =>
        val lastCorrectReview = correctReviews.head
        val intervals = (correctReviews.init zip correctReviews.tail).map {
          case (review1, review2) => new Interval(review1.reviewDate, review2.reviewDate)
        }
        val sortedIntervals = intervals.sortBy(i => i.toDuration.getMillis)
        val longestInterval = sortedIntervals.head
        lastCorrectReview.reviewDate.plus(longestInterval.toDuration.withDurationAdded(0, REVIEW_BACKOFF_FACTOR))
    }
  }
}

case class Review(reviewDate: DateTime, timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) {
  def this(timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) = this(DateTime.now(), timeOnFront, timeOnBack, difficulty)
}