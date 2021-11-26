package fbariy.seafight.server.application.game

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.std.Semaphore
import cats.implicits._
import fbariy.seafight.core.application.GameOutput
import fbariy.seafight.core.application.error.AppError
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.{Cell, PlayerWithGame}
import fbariy.seafight.server.application.back.BackToMoveValidator
import fbariy.seafight.server.application.notification.NotificationBus

class MoveHandler[F[_]: Sync](gameRepository: GameRepository[F],
                              moveValidator: MoveValidator,
                              backValidator: BackToMoveValidator[F],
                              bus: NotificationBus[F],
                              semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame],
             kick: Cell): F[ValidatedNec[AppError, GameOutput]] = {
    def makeMove: F[ValidatedNec[AppError, GameOutput]] =
      for {
        playerCtx   <- played
        validateRes <- validate(playerCtx)
        validated <- validateRes.traverse { _ =>
          val updatedCtx = playerCtx.addMove(kick)

          for {
            _ <- gameRepository.updateGame(updatedCtx.game)
            _ <- bus.enqueue(
              updatedCtx.game.id,
              updatedCtx,
              MoveMadeNotification(updatedCtx.p, updatedCtx.game.id, kick))
          } yield updatedCtx
        }
      } yield validated.map(GameOutput(_))

    semaphore.permit.use(_ => makeMove)
  }

  private def validate(
      playerCtx: PlayerWithGame): F[ValidatedNec[AppError, Unit]] = {
    val validators =
      (backValidator.backIsNotRequested(playerCtx.game),
       Sync[F].delay(moveValidator.gameIsNotOver(playerCtx.game)),
       Sync[F].delay(moveValidator.canMakeMove(playerCtx)))

    validators.mapN(_ |+| _ |+| _)
  }
}
