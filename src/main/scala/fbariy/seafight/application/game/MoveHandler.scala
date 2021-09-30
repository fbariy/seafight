package fbariy.seafight.application.game

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.effect.{Concurrent, Sync}
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.errors.AppError
import fbariy.seafight.domain.{Cell, Turn}
import fbariy.seafight.infrastructure.PlayerWithGame

class MoveHandler[F[_]: Concurrent](gameRepository: GameRepository[F],
                                    validator: MoveValidator,
                                    semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame],
             kick: Cell): F[ValidatedNec[AppError, GameOutput]] = {
    def makeMove: F[ValidatedNec[AppError, GameOutput]] =
      for {
        playerWithGame <- played
        validationRes <- Sync[F].delay(
          validator.canMakeMove(playerWithGame) |+| validator.gameIsNotOver(
            playerWithGame.game))
        validated <- validationRes match {
          case Valid(_) =>
            val newTurnSerial = playerWithGame.game.turns
              .maxByOption(_.serial)
              .map(_.serial + 1)
              .getOrElse(1)

            val newTurns = Turn(playerWithGame.p, kick, newTurnSerial) +: playerWithGame.game.turns

            gameRepository
              .updateTurns(playerWithGame.game.id, newTurns)
              .map { _ =>
                GameOutput(playerWithGame.updateTurns(newTurns)).validNec[AppError]
              }
          case i @ Invalid(_) => i.pure[F]
        }
      } yield validated

    semaphore withPermit makeMove
  }
}
