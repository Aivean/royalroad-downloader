name := "royalroadl-downloader"

version := "1.2.5"

scalaVersion := "2.11.11"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.2.1"
libraryDependencies += "org.rogach" %% "scallop" % "1.0.1"

mainClass in assembly := Some("com.aivean.royalroad.Main")