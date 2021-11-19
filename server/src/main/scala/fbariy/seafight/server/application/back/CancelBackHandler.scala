package fbariy.seafight.server.application.back

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.std.Semaphore
import cats.implicits._
import fbariy.seafight.core.application.error.AppError
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.PlayerWithGame
import fbariy.seafight.server.application.notification.NotificationBus

class CancelBackHandler[F[_]: Sync](validator: BackToMoveValidator[F],
                                    repository: BackToMoveRepository[F],
                                    bus: NotificationBus[F],
                                    semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame]): F[ValidatedNec[AppError, Unit]] = {
    def cancelBack: F[ValidatedNec[AppError, Unit]] =
      for {
        playerCtx <- played
        game = playerCtx.game
        validationRes <- validator.backRequested(playerCtx.game)

        result <- validationRes.traverse { _ =>
          for {
            _ <- repository.release(game.id)
            _ <- bus.enqueue(game.id,
                             playerCtx,
                             BackCanceledNotification(playerCtx.p, game.id))
          } yield ()
        }

      } yield result

    semaphore.permit.use(_ => cancelBack)
  }
}
