package fbariy.seafight
package client

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO.delay(println("Hello new client app")).as(ExitCode.Success)
}
