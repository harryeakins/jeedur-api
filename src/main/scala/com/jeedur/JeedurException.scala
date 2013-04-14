package com.jeedur

case class JeedurException(status_code: Int, message: String) extends Exception
