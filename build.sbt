import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

val CatsEffectVersion       = "3.2.9"
val Http4sVersion           = "0.23.6"
val CirceVersion            = "0.14.1"
val MunitVersion            = "0.7.28"
val LogbackVersion          = "1.2.3"
val MunitCatsEffectVersion  = "1.0.6"
val DoobieVersion           = "1.0.0-RC1"
val PureConfigVersion       = "0.17.0"
val FlyWayVersion           = "8.0.1"
val ScalaLoggingVersion     = "3.9.4"
val TestContainersVersion   = "0.39.6"
val ScalaMetaVersion        = "20.2.0"
val EnumeratumCirceVersion  = "1.7.0"
val KindProjectorVersion    = "0.13.4"
val BetterMonadicForVersion = "0.3.1"

ThisBuild / scalaVersion := "2.13.17"

lazy val runMigrate = taskKey[Unit]("Migrates the database schema.")

// for development purposes
lazy val experimental = project
  .settings(
    name := "seafight-experimental",
    Compile / mainClass := Some("fbariy.seafight.experimental.Main"),
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"         % "3.2.9",
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion)
  )

lazy val core = project
  .settings(
    name := "seafight-core",
    libraryDependencies ++= Seq(
      "io.circe"      %% "circe-core"          % CirceVersion,
      "io.circe"      %% "circe-generic"       % CirceVersion,
      "io.circe"      %% "circe-parser"        % CirceVersion,
      "com.beachape"  %% "enumeratum-circe"    % EnumeratumCirceVersion,
      "org.typelevel" %% "cats-effect"         % CatsEffectVersion,
      "org.http4s"    %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"    %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"    %% "http4s-circe"        % Http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % Http4sVersion
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion)
  )

lazy val client = project
  .dependsOn(core)
  .settings(
    name := "seafight-client",
    Compile / mainClass := Some("fbariy.seafight.client.Main"),
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"         % CatsEffectVersion,
      "org.http4s"            %% "http4s-blaze-client" % Http4sVersion,
      "io.circe"              %% "circe-core"          % CirceVersion,
      "io.circe"              %% "circe-generic"       % CirceVersion,
      "io.circe"              %% "circe-parser"        % CirceVersion,
      "com.github.pureconfig" %% "pureconfig"          % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion)
  )

lazy val server = project
  .dependsOn(core)
  .settings(
    name := "seafight-server",
    version := "0.0.1-SNAPSHOT",
    Compile / mainClass := Some("fbariy.seafight.server.Main"),
    Docker / packageName := "seafight-app",
    dockerEnvVars := Map("SEAFIGHT_ENV" -> "test"),
    fullRunTask(
      runMigrate,
      Compile,
      "fbariy.seafight.server.infrastructure.migration.DBMigrationsCommand"),
    fork / runMigrate := true,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-effect"                     % CatsEffectVersion,
      "org.http4s"                 %% "http4s-blaze-server"             % Http4sVersion,
      "org.http4s"                 %% "http4s-blaze-client"             % Http4sVersion,
      "org.http4s"                 %% "http4s-circe"                    % Http4sVersion,
      "org.http4s"                 %% "http4s-dsl"                      % Http4sVersion,
      "io.circe"                   %% "circe-core"                      % CirceVersion,
      "io.circe"                   %% "circe-generic"                   % CirceVersion,
      "io.circe"                   %% "circe-parser"                    % CirceVersion,
      "org.tpolecat"               %% "doobie-hikari"                   % DoobieVersion,
      "org.tpolecat"               %% "doobie-postgres"                 % DoobieVersion,
      "org.tpolecat"               %% "doobie-postgres-circe"           % DoobieVersion,
      "org.scalameta"              %% "munit"                           % MunitVersion % Test,
      "org.typelevel"              %% "munit-cats-effect-3"             % MunitCatsEffectVersion % Test,
      "ch.qos.logback"             % "logback-classic"                  % LogbackVersion,
      "com.github.pureconfig"      %% "pureconfig"                      % PureConfigVersion,
      "com.github.pureconfig"      %% "pureconfig-cats-effect"         % PureConfigVersion,
      "org.flywaydb"               % "flyway-core"                      % FlyWayVersion,
      "com.typesafe.scala-logging" %% "scala-logging"                   % ScalaLoggingVersion,
      "org.scalameta"              %% "svm-subs"                        % ScalaMetaVersion,
      "com.dimafeng"               %% "testcontainers-scala-munit"      % TestContainersVersion % "test",
      "com.dimafeng"               %% "testcontainers-scala-postgresql" % TestContainersVersion % "test"
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .enablePlugins(JavaServerAppPackaging)
