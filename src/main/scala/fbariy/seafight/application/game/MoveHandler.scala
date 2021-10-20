package fbariy.seafight.application.game

import cats.data.ValidatedNec
import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import fbariy.seafight.application.back.BackToMoveValidator
import fbariy.seafight.application.error.AppError
import fbariy.seafight.domain.GameWithPlayers._
import fbariy.seafight.domain.{Cell, Turn}
import fbariy.seafight.infrastructure.PlayerWithGame

class MoveHandler[F[_]: Concurrent](gameRepository: GameRepository[F],
                                    moveValidator: MoveValidator,
                                    backValidator: BackToMoveValidator[F],
                                    semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame],
             kick: Cell): F[ValidatedNec[AppError, GameOutput]] = {
    def makeMove: F[ValidatedNec[AppError, GameOutput]] =
      for {
        playerCtx   <- played
        validateRes <- validate(playerCtx)
        validated <- validateRes.traverse { _ =>
          val updatedPlayerCtx = updateGame(playerCtx, kick)
          val updatedGame      = updatedPlayerCtx.game

          gameRepository
            .updateGame(updatedGame.id,
                        Some(updatedGame.turns),
                        updatedGame.winner)
            .map(_ => updatedPlayerCtx)
        }
      } yield validated.map(GameOutput(_))

    semaphore withPermit makeMove
  }

  private def validate(
      playerCtx: PlayerWithGame): F[ValidatedNec[AppError, Unit]] = {
    val validators =
      (backValidator.backIsNotRequested(playerCtx.game),
       Sync[F].delay(moveValidator.gameIsNotOver(playerCtx.game)),
       Sync[F].delay(moveValidator.canMakeMove(playerCtx)))

    validators.mapN(_ |+| _ |+| _)
  }

  def updateGame(playerCtx: PlayerWithGame, kick: Cell): PlayerWithGame = {
    val game         = playerCtx.game
    val updatedMoves = Turn(playerCtx.p, kick, game.nextSerial) +: game.turns
    val maybeWinner =
      checkWinner(updatedMoves, game.p1, game.p1Ships, game.p2, game.p2Ships)

    playerCtx.copy(game = game.copy(turns = updatedMoves, winner = maybeWinner))
  }
}
