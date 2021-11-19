package fbariy.seafight.server.application.ship

import cats.Applicative
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.std.Semaphore
import cats.implicits._
import fbariy.seafight.core.application
import fbariy.seafight.core.application.{AddShipsOutput, InviteOutput}
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.application.notification._
import fbariy.seafight.server.application.game.GameRepository
import fbariy.seafight.server.application.notification._
import fbariy.seafight.core.domain.{Cell, Game, PlayerWithInvite}

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
          } yield application.AddShipsOutput(InviteOutput(invite), p, ships)
        }
      } yield validated

    semaphore.permit.use(_ => createShips)
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
