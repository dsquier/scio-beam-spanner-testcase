import sbt._

object Dependencies {
  val scioVersion = "0.5.7-SNAPSHOT"
  val beamVersion = "2.6.0"
  val defaultScalaVersion = "2.12.6"
  val scalaMacrosVersion = "2.1.1"
  val circeVersion = "0.9.2"
  val slf4jVersion = "1.7.25"

  lazy val commonDependencies = Seq(
    "com.spotify" %% "scio-core" % scioVersion,
    "com.spotify" %% "scio-test" % scioVersion % "test",
    "org.apache.beam" % "beam-runners-direct-java" % beamVersion,
    "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion,
    "org.apache.beam" % "beam-sdks-java-io-google-cloud-platform" % beamVersion,
    "org.slf4j" % "slf4j-simple" % slf4jVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  lazy val paradiseDependency = "org.scalamacros" % "paradise" % scalaMacrosVersion cross CrossVersion.full

  lazy val macroDependencies = "org.scala-lang" % "scala-reflect" % defaultScalaVersion
}