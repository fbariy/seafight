package fbariy.seafight.server.application.back

import cats.Monad
import cats.data.ValidatedNec
import cats.effect.MonadCancel
import cats.effect.std.Semaphore
import cats.implicits._
import fbariy.seafight.core.application.GameOutput
import fbariy.seafight.core.application.error.AppError
import fbariy.seafight.server.application.game.GameRepository
import fbariy.seafight.server.application.notification.NotificationBus
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.{Player, PlayerWithGame, Turn}

class AcceptBackHandler[F[_]: Monad: MonadCancel[*[_], Throwable]](
    validator: BackToMoveValidator[F],
    backRepository: BackToMoveRepository[F],
    gameRepository: GameRepository[F],
    bus: NotificationBus[F],
    semaphore: Semaphore[F]) {
  def handle(
      played: F[PlayerWithGame]): F[ValidatedNec[AppError, GameOutput]] = {
    def accept: F[ValidatedNec[AppError, GameOutput]] =
      for {
        playerCtx     <- played
        validationRes <- validate(playerCtx)
        result <- validationRes.traverse { playerWithTurn =>
          val updatedMoves =
            playerCtx.game.turns.filter(_.serial <= playerWithTurn._2.serial)
          val updatedCtx = playerCtx.updateMoves(updatedMoves)
          val game       = updatedCtx.game

          for {
            _ <- gameRepository.updateGame(game)
            _ <- backRepository.release(game.id)
            _ <- bus.enqueue(game.id,
                             updatedCtx,
                             BackAcceptedNotification(updatedCtx.p, game.id))
          } yield GameOutput(updatedCtx)
        }
      } yield result

    semaphore.permit.use(_ => accept)
  }

  private def validate(
      playerCtx: PlayerWithGame): F[ValidatedNec[AppError, (Player, Turn)]] =
    validator
      .backRequested(playerCtx.game)
      .map { backRequested =>
        backRequested.andThen { playerWithTurn =>
          validator
            .isOpponentAccepts(playerCtx.p, playerWithTurn._1)
            .map(_ => playerWithTurn)
        }
      }
}
