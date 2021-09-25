package fbariy.seafight.infrastructure.endpoint

import cats.effect.Sync
import fbariy.seafight.application.game.CanMakeMoveHandler
import fbariy.seafight.infrastructure.PlayerWithGame
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

class GameEndpoints[F[_]: Sync] extends Http4sDsl[F] {
  def canMakeMove(
      handler: CanMakeMoveHandler): AuthedRoutes[PlayerWithGame, F] =
    AuthedRoutes.of[PlayerWithGame, F] {
      case GET -> Root / "can-make-move" as played =>
        Ok(handler.handle(played))
    }
}
