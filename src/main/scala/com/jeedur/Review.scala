package com.jeedur

import org.joda.time.DateTime
import net.liftweb.json._
import scala.Some

object Difficulty extends Enumeration {
  type Difficulty = Value
  val EASY = Value("EASY")
  val FAIL = Value("FAIL")
}

import Difficulty._

object Review {
  def unapply(s: String): Option[Review] = {
    implicit val formats = DefaultFormats + new ReviewSerializer
    val p = parse(s)
    try {
      Some(p.extract[Review])
    } catch {
      case e: MappingException => throw new JeedurException(400, ErrorMessages.REQUIRED_FIELD_NOT_PRESENT)
    }
  }
}

class ReviewSerializer extends Serializer[Review] {
  private val ReviewClass = classOf[Review]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Review] = {
    case (TypeInfo(ReviewClass, _), json) => json match {
      case JObject(JField("timeOnFront", JDouble(f)) :: JField("timeOnBack", JDouble(b)) :: JField("difficulty", JString(d)) :: Nil) =>
        new Review(f, b, Difficulty.withName(d))
      case JObject(JField("timeOnFront", JInt(f)) :: JField("timeOnBack", JInt(b)) :: JField("difficulty", JString(d)) :: Nil) =>
        new Review(f.toDouble, b.toDouble, Difficulty.withName(d))
      case JObject(JField("timeOnFront", JDouble(f)) :: JField("timeOnBack", JDouble(b)) :: JField("reviewDate", JString(date)) :: JField("difficulty", JString(d)) :: Nil) =>
        new Review(DateTime.parse(date), f, b, Difficulty.withName(d))
      case x => throw new MappingException("Can't convert " + x + " to Review")
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case x: Review =>
      JObject(JField("timeOnFront", JDouble(x.timeOnFront)) ::
        JField("timeOnBack", JDouble(x.timeOnBack)) ::
        JField("reviewDate", JString(x.reviewDate.toString)) ::
        JField("difficulty", JString(x.difficulty.toString)) :: Nil)
  }
}

class Review(val reviewDate: DateTime, val timeOnFront: Double, val timeOnBack: Double, val difficulty: Difficulty) {
  def this(timeOnFront: Double, timeOnBack: Double, difficulty: Difficulty) = this(DateTime.now(), timeOnFront, timeOnBack, difficulty)
}
