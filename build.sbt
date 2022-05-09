import Dependencies._

ThisBuild / scalaVersion := "2.13.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

// Core with minimal dependencies, enough to spawn your first bot.
libraryDependencies += "com.bot4s" %% "telegram-core" % "5.4.2"

// Extra goodies: Webhooks, support for games, bindings for actors.
libraryDependencies += "com.bot4s" %% "telegram-akka" % "5.4.2"

// Add postgres db support
libraryDependencies += "org.postgresql" % "postgresql" % "9.3-1102-jdbc41"

lazy val root = (project in file("."))
  .settings(
    name := "voting-tg-bot",
    assembly / assemblyJarName := "votingBotExe.jar"
  )
