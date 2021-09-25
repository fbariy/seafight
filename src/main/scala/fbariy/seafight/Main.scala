package fbariy.seafight

import cats.effect.{ExitCode, IO, IOApp}
import fbariy.seafight.infrastructure.GameServer

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    GameServer.resource[IO].use(_ => IO.never).as(ExitCode.Success)
}
