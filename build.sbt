name := "royalroad-downloader"

version := "2.0.0"

scalaVersion := "2.11.11"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.2.1"
libraryDependencies += "org.rogach" %% "scallop" % "1.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

mainClass in (Compile, run) := Some("com.aivean.royalroad.Main")
mainClass in assembly := Some("com.aivean.royalroad.Main")