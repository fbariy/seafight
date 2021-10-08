package fbariy.seafight.application.ship

import cats.Applicative
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.effect.Sync
import cats.effect.concurrent.Semaphore
import cats.implicits._
import fbariy.seafight.application.errors._
import fbariy.seafight.application.game.GameRepository
import fbariy.seafight.application.invite.InviteOutput
import fbariy.seafight.domain.{Cell, Game}
import fbariy.seafight.infrastructure.PlayerWithInvite

import java.util.UUID

class AddShipsHandler[F[_]: Sync](shipsRepo: ShipsRepository[F],
                                  gameRepo: GameRepository[F],
                                  validator: AddShipsValidator[F],
                                  semaphore: Semaphore[F]) {
  def handle(
      ships: Seq[Cell],
      invited: PlayerWithInvite): F[ValidatedNec[AppError, AddShipsOutput]] = {
    import invited._

    def addShips: F[ValidatedNec[AppError, AddShipsOutput]] =
      for {
        shipsAreCorrect <- validator
          .gameIsNotCreated(invite.id)
          .map(_ |+| validator.correctedShips(ships))
        validated <- shipsAreCorrect match {
          case Valid(_) =>
            for {
              _              <- shipsRepo.add(invite, p, p == invite.p1, ships)
              maybePairShips <- shipsRepo.release(invite)
              _              <- gameCreatorIfShipsAreDone(maybePairShips)(invite.id)
            } yield Valid(AddShipsOutput(InviteOutput(invite), p, ships))
          case i @ Invalid(_) => i.pure[F]
        }
      } yield validated

    semaphore withPermit addShips
  }

  val gameCreatorIfShipsAreDone
    : PartialFunction[Option[(PlayerShips, PlayerShips)], UUID => F[_]] = {
    case Some((p1Ships, p2Ships)) =>
      id =>
        gameRepo.add(Game(id, p1Ships.ships, p2Ships.ships, Seq()))
    case None =>
      _ =>
        Applicative[F].unit
  }
}
