package com.jeedur

import org.joda.time.DateTime

object Difficulty extends Enumeration {
  type Difficulty = Value
  val EASY = Value("EASY")
  val FAIL = Value("FAIL")
}

import Difficulty._

case class Review(reviewDate: DateTime, timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) {
  def this(timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) = this(DateTime.now(), timeOnFront, timeOnBack, difficulty)
}