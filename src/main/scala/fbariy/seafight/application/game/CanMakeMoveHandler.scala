package fbariy.seafight.application.game

import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.infrastructure.PlayerWithGame

class CanMakeMoveHandler[F[_]: Sync](validator: MoveValidator) {
  def handle(played: F[PlayerWithGame]): F[Boolean] =
    for {
      playerWithGame <- played
      validated      <- Sync[F].delay(validator.canMakeMove(playerWithGame))
    } yield validated.isValid
}
