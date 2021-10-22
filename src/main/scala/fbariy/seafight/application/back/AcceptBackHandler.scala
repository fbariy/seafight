package fbariy.seafight.application.back

import cats.Monad
import cats.data.ValidatedNec
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.error.AppError
import fbariy.seafight.application.game.{GameOutput, GameRepository}
import fbariy.seafight.application.notification.{
  BackAcceptedNotification,
  NotificationBus
}
import fbariy.seafight.domain.{Player, Turn}
import fbariy.seafight.infrastructure.PlayerWithGame

class AcceptBackHandler[F[_]: Monad](validator: BackToMoveValidator[F],
                                     backRepository: BackToMoveRepository[F],
                                     gameRepository: GameRepository[F],
                                     bus: NotificationBus[F],
                                     semaphore: Semaphore[F]) {
  def handle(played: F[PlayerWithGame]): F[ValidatedNec[AppError, GameOutput]] = {
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
            _ <- bus.enqueue(BackAcceptedNotification(updatedCtx.p, game.id))
          } yield GameOutput(updatedCtx)
        }
      } yield result

    semaphore withPermit accept
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
