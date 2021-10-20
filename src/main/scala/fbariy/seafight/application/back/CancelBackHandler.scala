package fbariy.seafight.application.back

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.error.{AppError, BackNotRequestedError}
import fbariy.seafight.application.notification.{
  CancelBackNotification,
  NotificationBus
}
import fbariy.seafight.infrastructure.PlayerWithGame

class CancelBackHandler[F[_]: Sync](repository: BackToMoveRepository[F],
                                    bus: NotificationBus[F],
                                    semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame]): F[ValidatedNec[AppError, Unit]] = {
    def cancelBack: F[ValidatedNec[AppError, Unit]] =
      for {
        playerCtx     <- played
        maybeReleased <- repository.release(playerCtx.game.id)

        validationRes = maybeReleased
          .toRight(BackNotRequestedError)
          .toValidatedNec

        result <- validationRes.traverse { playerAndTurn =>
          bus
            .enqueue(
              CancelBackNotification(playerAndTurn._1, playerCtx.game.id))
            .map(_ => ())
        }

      } yield result

    semaphore withPermit cancelBack
  }
}
