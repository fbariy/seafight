package fbariy.seafight.server.application.back

import cats.data.ValidatedNec
import cats.effect.{MonadCancel, Sync}
import cats.effect.std.Semaphore
import cats.implicits._
import fbariy.seafight.core.application.TurnOutput
import fbariy.seafight.core.application.error.AppError
import fbariy.seafight.server.application.notification.NotificationBus
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.{PlayerWithGame, Turn}

class BackToMoveHandler[F[_]: Sync: MonadCancel[*[_], Throwable]](
    validator: BackToMoveValidator[F],
    backToMoveRepository: BackToMoveRepository[F],
    bus: NotificationBus[F],
    semaphore: Semaphore[F]
) {
  def handle(played: F[PlayerWithGame],
             moveNumber: Int): F[ValidatedNec[AppError, Unit]] = {
    def requestBack: F[ValidatedNec[AppError, Unit]] =
      for {
        playerCtx <- played
        game = playerCtx.game

        validateResult <- validate(playerCtx, moveNumber)
        result <- validateResult.traverse { turn =>
          for {
            _ <- backToMoveRepository.add(game.id, playerCtx.p, turn)
            _ <- bus.enqueue(
              game.id,
              playerCtx,
              BackRequestedNotification(playerCtx.p, game.id, TurnOutput(turn)))
          } yield ()
        }
      } yield result

    semaphore.permit.use(_ => requestBack)
  }

  private def validate(playerCtx: PlayerWithGame,
                       moveNumber: Int): F[ValidatedNec[AppError, Turn]] = {
    val validators = (
      validator.backIsNotRequested(playerCtx.game),
      Sync[F].delay(validator.moveIsExist(playerCtx.game, moveNumber))
    )

    validators.mapN { (backIsNotRequested, moveIsExist) =>
      (backIsNotRequested product moveIsExist).map(_._2)
    }
  }
}
