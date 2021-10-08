import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

val CatsEffectVersion      = "2.5.1"
val Http4sVersion          = "0.22.6"
val CirceVersion           = "0.13.0"
val MunitVersion           = "0.7.28"
val LogbackVersion         = "1.2.3"
val MunitCatsEffectVersion = "1.0.4"
val DoobieVersion          = "0.12.1"
val PureConfigVersion      = "0.16.0"
val FlyWayVersion          = "7.2.0"
val ScalaLoggingVersion    = "3.9.4"
val TestContainersVersion  = "0.39.6"
val ScalaMetaVersion       = "20.2.0"
val EnumeratumCirceVersion = "1.7.0"

lazy val runMigrate = taskKey[Unit]("Migrates the database schema.")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
enablePlugins(JavaServerAppPackaging)

lazy val root = (project in file("."))
  .settings(
    organization := "fbariy",
    name := "seafight",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    Compile / mainClass := Some("fbariy.seafight.Main"),
    Docker / packageName := "seafight-app",
    dockerEnvVars := Map("SEAFIGHT_ENV" -> "test"),
    fullRunTask(runMigrate,
                Compile,
                "fbariy.seafight.infrastructure.migration.DBMigrationsCommand"),
    fork / runMigrate := true,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-effect"                     % CatsEffectVersion,
      "org.http4s"                 %% "http4s-blaze-server"             % Http4sVersion,
      "org.http4s"                 %% "http4s-blaze-client"             % Http4sVersion,
      "org.http4s"                 %% "http4s-circe"                    % Http4sVersion,
      "org.http4s"                 %% "http4s-dsl"                      % Http4sVersion,
      "io.circe"                   %% "circe-generic"                   % CirceVersion,
      "org.tpolecat"               %% "doobie-hikari"                   % DoobieVersion,
      "org.tpolecat"               %% "doobie-postgres"                 % DoobieVersion,
      "org.tpolecat"               %% "doobie-postgres-circe"           % DoobieVersion,
      "org.scalameta"              %% "munit"                           % MunitVersion % Test,
      "org.typelevel"              %% "munit-cats-effect-2"             % MunitCatsEffectVersion % Test,
      "ch.qos.logback"             % "logback-classic"                  % LogbackVersion,
      "com.github.pureconfig"      %% "pureconfig"                      % PureConfigVersion,
      "com.github.pureconfig"      %% "pureconfig-cats-effect2"         % PureConfigVersion,
      "org.flywaydb"               % "flyway-core"                      % FlyWayVersion,
      "com.typesafe.scala-logging" %% "scala-logging"                   % ScalaLoggingVersion,
      "org.scalameta"              %% "svm-subs"                        % ScalaMetaVersion,
      "com.beachape"               %% "enumeratum-circe"                % EnumeratumCirceVersion,
      "com.dimafeng"               %% "testcontainers-scala-munit"      % TestContainersVersion % "test",
      "com.dimafeng"               %% "testcontainers-scala-postgresql" % TestContainersVersion % "test"
    ),
    addCompilerPlugin(
      "org.typelevel"              %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
