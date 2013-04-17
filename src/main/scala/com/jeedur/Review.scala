package com.jeedur

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: harry
 * Date: 17/04/2013
 * Time: 23:26
 * To change this template use File | Settings | File Templates.
 */

object Difficulty extends Enumeration {
  type Difficulty = Value
  val EASY, FAIL = Value
}

import Difficulty._


class Review(val review_date: DateTime, val timeOnFront: Float, val timeOnBack: Float, val difficulty: Difficulty) {
  override def toString = {
    "Review(" + review_date + ", " + timeOnBack + ", " + timeOnFront + ", " + difficulty + ")"
  }
}
