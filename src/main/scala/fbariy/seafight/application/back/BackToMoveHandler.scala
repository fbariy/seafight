package fbariy.seafight.application.back

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.error.AppError
import fbariy.seafight.application.game.TurnOutput
import fbariy.seafight.application.notification.{
  BackRequestedNotification,
  NotificationBus
}
import fbariy.seafight.domain.Turn
import fbariy.seafight.infrastructure.PlayerWithGame

class BackToMoveHandler[F[_]: Sync](
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
              BackRequestedNotification(playerCtx.p, game.id, TurnOutput(turn)))
          } yield ()
        }
      } yield result

    semaphore.withPermit(requestBack)
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
