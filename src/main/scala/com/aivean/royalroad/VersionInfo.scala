package com.aivean.royalroad

object VersionInfo {
  lazy val version: String = try {
    val props = new java.util.Properties()
    props.load(getClass.getResourceAsStream("/version.properties"))
    props.getProperty("version")
  } catch {
    case _: Throwable => "unknown"
  }
}