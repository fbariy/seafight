package fbariy.seafight.application.back

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.error.AppError
import fbariy.seafight.application.notification.{
  BackCanceled,
  NotificationBus
}
import fbariy.seafight.infrastructure.PlayerWithGame

class CancelBackHandler[F[_]: Sync](validator: BackToMoveValidator[F],
                                    repository: BackToMoveRepository[F],
                                    bus: NotificationBus[F],
                                    semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame]): F[ValidatedNec[AppError, Unit]] = {
    def cancelBack: F[ValidatedNec[AppError, Unit]] =
      for {
        playerCtx     <- played
        validationRes <- validator.backRequested(playerCtx.game)

        result <- validationRes.traverse { _ =>
          for {
            _ <- repository.release(playerCtx.game.id)
            _ <- bus.enqueue(
              BackCanceled(playerCtx.p, playerCtx.game.id))
          } yield ()
        }

      } yield result

    semaphore withPermit cancelBack
  }
}