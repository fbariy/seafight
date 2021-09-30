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
  def handle(played: PlayerWithGame,
             kick: Cell): F[ValidatedNec[AppError, GameOutput]] = {
    def makeMove: F[ValidatedNec[AppError, GameOutput]] =
      for {
        //todo: хардкод, вместо PlayerWithGame, принимать F[PlayerWithGame] или F[ValidatedNec[AppError, PlayerWithGame]]
        actualGame <- gameRepository.find(played.game.id)
        actualPlayed = played.copy(game = actualGame.get)

        validationRes <- Sync[F].delay(
          validator.canMakeMove(actualPlayed) |+| validator.gameIsNotOver(
            actualPlayed.game))
        validated <- validationRes match {
          case Valid(_) =>
            val newTurnSerial = actualPlayed.game.turns
              .maxByOption(_.serial)
              .map(_.serial + 1)
              .getOrElse(1)

            val newTurns = Turn(actualPlayed.p, kick, newTurnSerial) +: actualPlayed.game.turns

            gameRepository
              .updateTurns(actualPlayed.game.id, newTurns)
              .map { _ =>
                GameOutput(played.updateTurns(newTurns)).validNec[AppError]
              }
          case i @ Invalid(_) => i.pure[F]
        }
      } yield validated

    semaphore withPermit makeMove
  }
}
