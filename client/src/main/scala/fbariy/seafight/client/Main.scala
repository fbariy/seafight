package fbariy.seafight
package client

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    new GameClient[IO].resource.use(_ => IO.unit).as(ExitCode.Success)
}
