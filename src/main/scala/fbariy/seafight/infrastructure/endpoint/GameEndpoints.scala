package fbariy.seafight.infrastructure.endpoint

import cats.effect.Sync
import fbariy.seafight.application.game.{CanMakeMoveHandler, MoveHandler}
import fbariy.seafight.domain.Cell
import fbariy.seafight.infrastructure.PlayerWithGame
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import fbariy.seafight.infrastructure.codec._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._

class GameEndpoints[F[_]: Sync] extends Http4sDsl[F] {
  def canMakeMove(
      handler: CanMakeMoveHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case GET -> Root / "can-make-move" as played =>
        for {
          canMakeMove <- handler.handle(played)
          response    <- Ok(canMakeMove)
        } yield response
    }

  def move(handler: MoveHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case authReq @ POST -> Root / "move" as played =>
        for {
          kick      <- authReq.req.as[Cell]
          validated <- handler.handle(played, kick)
          response  <- Ok(validated)
        } yield response
    }
}
