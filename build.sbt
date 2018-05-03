import sbt._
import Keys._
import Dependencies._

lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  resolvers             += Resolver.sonatypeRepo("snapshots"),
  organization          := "com.joinhoney",
  version               := "1.1.0",
  scalaVersion          := defaultScalaVersion,
  scalacOptions         ++= Seq("-target:jvm-1.8",
                                "-deprecation",
                                "-feature",
                                "-unchecked"),
  javacOptions          ++= Seq("-source", "1.8",
                                "-target", "1.8")
)

lazy val macroSettings = Seq(
  libraryDependencies += macroDependencies,
  addCompilerPlugin(paradiseDependency)
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val droplistMigration = (project in file("."))
  .settings(
    commonSettings ++ macroSettings ++ noPublishSettings,
    description := "Droplist v3 Migration",
    libraryDependencies ++= commonDependencies
  )
  .enablePlugins(PackPlugin)
