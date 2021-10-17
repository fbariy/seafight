package fbariy.seafight.application.game

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import fbariy.seafight.application.error.AppError
import fbariy.seafight.domain.{Cell, GameWithPlayers, Player, Turn}
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
          validator.gameIsNotOver(playerWithGame.game) |+| validator
            .canMakeMove(playerWithGame))
        validated <- validationRes match {
          case Valid(_) =>
            val newTurnSerial = playerWithGame.game.turns
              .maxByOption(_.serial)
              .map(_.serial + 1)
              .getOrElse(1)

            val newTurns              = Turn(playerWithGame.p, kick, newTurnSerial) +: playerWithGame.game.turns
            val updatedPlayerWithGame = playerWithGame.updateTurns(newTurns)

            gameRepository
              .updateGame(playerWithGame.game.id,
                          Some(newTurns),
                          checkWinner(updatedPlayerWithGame.game))
              .map(updated =>
                GameOutput(updatedPlayerWithGame.copy(game = updated))
                  .validNec[AppError])

          case i @ Invalid(_) => i.pure[F]
        }
      } yield validated

    semaphore withPermit makeMove
  }

  private def checkWinner(game: GameWithPlayers): Option[Player] =
    if (checkGameOver(game.p1Ships, game.getPlayerTurns(game.p2).map(_.kick)))
      Some(game.p2)
    else if (checkGameOver(game.p2Ships,
                           game.getPlayerTurns(game.p1).map(_.kick)))
      Some(game.p1)
    else None

  private def checkGameOver(ships: Seq[Cell], kicks: Seq[Cell]): Boolean =
    if (kicks.size < 20) false
    else (kicks intersect ships).size >= 20
}
