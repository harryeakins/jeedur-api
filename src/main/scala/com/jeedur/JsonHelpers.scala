package com.jeedur

trait JsonHelpers {

  import net.liftweb.json._
  import net.liftweb.json.ext._
  import net.liftweb.json.Extraction._

  implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all + new EnumNameSerializer(Difficulty)

  object Json {
    def apply(json: JValue, compacting: Boolean) = {
      val doc = render(json)
      if (compacting) compact(doc) else pretty(doc)
    }

    def apply(a: Any): Any = apply(decompose(a), compacting = true)
  }

}
