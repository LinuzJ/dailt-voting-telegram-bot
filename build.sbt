import Dependencies._

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "voting-tg-bot",
    libraryDependencies += scalaTest % Test
  )

// Core with minimal dependencies, enough to spawn your first bot.
libraryDependencies += "com.bot4s" %% "telegram-core" % "5.3.0"

// Extra goodies: Webhooks, support for games, bindings for actors.
libraryDependencies += "com.bot4s" %% "telegram-akka" % "5.3.0"
