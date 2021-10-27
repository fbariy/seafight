package fbariy.seafight.application.game

import cats.data.ValidatedNec
import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, Sync}
import cats.implicits._
import fbariy.seafight.application.back.BackToMoveValidator
import fbariy.seafight.application.error.AppError
import fbariy.seafight.application.notification.{
  MoveMadeNotification,
  NotificationBus
}
import fbariy.seafight.domain.Cell
import fbariy.seafight.infrastructure.PlayerWithGame

class MoveHandler[F[_]: Concurrent](gameRepository: GameRepository[F],
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
              MoveMadeNotification(updatedCtx.p, updatedCtx.game.id))
          } yield updatedCtx
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
}
