name := "royalroad-downloader"

version := "SNAPSHOT"

scalaVersion := "2.11.11"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.2.1"
libraryDependencies += "org.rogach" %% "scallop" % "1.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.11" % "0.9.1"

// inject version info
resourceGenerators in Compile += Def.task {
  val file = (resourceManaged in Compile).value / "version.properties"
  val props = new java.util.Properties()
  props.put("version", version.value)
  IO.write(props, "Version information", file)
  Seq(file)
}.taskValue

mainClass in (Compile, run) := Some("com.aivean.royalroad.Main")
mainClass in assembly := Some("com.aivean.royalroad.Main")