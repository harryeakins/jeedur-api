package com.jeedur

import javax.servlet.http.HttpServletRequest

/**
 * Created with IntelliJ IDEA.
 * User: harry
 * Date: 20/04/2013
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
object JeedurUtils {
  def getParameter(request: HttpServletRequest, key: String): Option[String] = {
    val string = request.getParameter(key)
    if (string == null) {
      return None
    } else {
      return Some(string)
    }
  }
}
