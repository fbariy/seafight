package fbariy.seafight.infrastructure.migration

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.typesafe.scalalogging.LazyLogging
import fbariy.seafight.infrastructure.GameServer

import scala.io.StdIn._

import scala.concurrent.ExecutionContext.global

object DBMigrationsCommand extends IOApp with LazyLogging {
  def run(args: List[String]): IO[ExitCode] =
    for {
      _      <- IO(print("[1] migrate\n[2] clean\nChoose an action: "))
      action <- IO(readInt())
      cfg    <- GameServer.config[IO](Blocker.liftExecutionContext(global))
      _ <- action match {
        case 1 => DBMigrations.migrate[IO](cfg.db)
        case 2 => DBMigrations.clean[IO](cfg.db)
        case _ =>
          IO.raiseError(
            new IllegalArgumentException("Action must be specified"))
      }
    } yield ExitCode.Success
}
