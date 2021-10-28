package fbariy.seafight.application.ship

import cats.Applicative
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.error._
import fbariy.seafight.application.game.GameRepository
import fbariy.seafight.application.invite.InviteOutput
import fbariy.seafight.application.notification._
import fbariy.seafight.domain.{Cell, Game}
import fbariy.seafight.infrastructure.PlayerWithInvite

final class AddShipsHandler[F[_]: Sync](shipsRepo: ShipsRepository[F],
                                        gameRepo: GameRepository[F],
                                        validator: AddShipsValidator[F],
                                        bus: NotificationBus[F],
                                        semaphore: Semaphore[F]) {
  def handle(ships: Seq[Cell], inviteCtx: PlayerWithInvite)
    : F[ValidatedNec[AppError, AddShipsOutput]] = {

    import inviteCtx._

    def createShips: F[ValidatedNec[AppError, AddShipsOutput]] =
      for {
        validationRes <- validate(inviteCtx, ships)
        validated <- validationRes.traverse { _ =>
          for {
            _              <- shipsRepo.add(invite, p, p == invite.p1, ships)
            maybePairShips <- shipsRepo.release(invite)
            _              <- gameCreatorIfShipsAreDone(maybePairShips)(inviteCtx)
            _ <- bus.enqueue(invite.id,
                             inviteCtx,
                             ShipsAddedNotification(inviteCtx.p, invite.id))
          } yield AddShipsOutput(InviteOutput(invite), p, ships)
        }
      } yield validated

    semaphore withPermit createShips
  }

  private def validate(inviteCtx: PlayerWithInvite,
                       ships: Seq[Cell]): F[ValidatedNec[AppError, Unit]] = {
    val validators = (
      validator.gameIsNotCreated(inviteCtx.invite.id),
      Sync[F].delay(validator.correctedShips(ships))
    )

    validators.mapN(_ |+| _)
  }

  private val gameCreatorIfShipsAreDone
    : PartialFunction[Option[(PlayerShips, PlayerShips)],
                      PlayerWithInvite => F[Unit]] = {
    case Some((p1Ships, p2Ships)) =>
      inviteCtx =>
        val inviteId = inviteCtx.invite.id

        for {
          _ <- gameRepo.add(Game(inviteId, p1Ships.ships, p2Ships.ships, Seq()))
          _ <- bus.enqueue(inviteId,
                           inviteCtx,
                           GameCreatedNotification(inviteId))
        } yield ()

    case None =>
      _ =>
        Applicative[F].unit
  }
}
