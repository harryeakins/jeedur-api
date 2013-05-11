package com.jeedur

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.{Matcher, MatchResult}
import org.joda.time.{Duration, DateTime, Interval}

trait CustomDateTimeMatchers {
  def roughlyEqual(right: DateTime): Matcher[DateTime] =
    new Matcher[DateTime] {
      def apply(left: DateTime) = {
        val interval = if (right.getMillis > left.getMillis) new Interval(left, right) else new Interval(right, left)
        MatchResult(interval.toDurationMillis < Duration.standardMinutes(1).getMillis,
          left.toString() + "does not roughly equal " + right.toString(),
          left.toString() + "roughly equals " + right.toString())
      }
    }
}

class ReviewUnitTest extends ScalatraFunSuite with JsonHelpers with BeforeAndAfterEach with CustomDateTimeMatchers {
  test("No reviews, next study time is time 0") {
    val nextStudyTime = Review.getNextStudyTime(List())
    nextStudyTime should roughlyEqual(new DateTime(0))
  }

  test("First correct review, next study time is in 2-4 hours") {
    val nextStudyTime = Review.getNextStudyTime(List(new Review(23.0, 12.0, Difficulty.EASY)))
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(3))
  }

  test("Two correct reviews in quick succession, next study time is still within 2-4 hours") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusSeconds(2), 23.0, 12.0, Difficulty.EASY)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(3))
  }

  test("Two correct reviews 4 hours apart, next review 12 hours after last review") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(4), 23.0, 12.0, Difficulty.EASY)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(4).plusHours(12))
  }

  test("Two correct reviews 4 hours apart, followed by a quick re-review, next review still 12 hours after last review") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(4), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(4).plusSeconds(2), 23.0, 12.0, Difficulty.EASY)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(4).plusHours(12))
  }

  test("One failed review, immediately re-review") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.FAIL)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now())
  }

  test("One good review, followed by a failed review 4 hours later, immediately re-review") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(4), 23.0, 12.0, Difficulty.FAIL)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(4))
  }

  test("Lots of good reviews, followed by a failed review 4 hours later, immediately re-review") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(3), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(10), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(30), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(90), 23.0, 12.0, Difficulty.FAIL)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(90))
  }

  test("Lots of good reviews, followed by a failed review 4 hours later, followed by more good reviews") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(3), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(10), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(30), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(90), 23.0, 12.0, Difficulty.FAIL),
      new Review(DateTime.now().plusHours(91), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(94), 23.0, 12.0, Difficulty.EASY)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(103))
  }

  test("It shouldn't matter which way the reviews are in the list") {
    val reviews = List(
      new Review(DateTime.now(), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(90), 23.0, 12.0, Difficulty.FAIL),
      new Review(DateTime.now().plusHours(3), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(91), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(30), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(94), 23.0, 12.0, Difficulty.EASY),
      new Review(DateTime.now().plusHours(10), 23.0, 12.0, Difficulty.EASY)
    )
    val nextStudyTime = Review.getNextStudyTime(reviews)
    nextStudyTime should roughlyEqual(DateTime.now().plusHours(103))
  }
}
