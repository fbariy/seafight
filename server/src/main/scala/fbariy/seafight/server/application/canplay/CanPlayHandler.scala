package fbariy.seafight
package server
package application.canplay

import cats.data.ValidatedNec
import cats.effect.Concurrent
import cats.implicits._
import cats.{Applicative, FlatMap}
import fbariy.seafight.core.application.error.{AppError, ShipsAreNotSetupError}
import fbariy.seafight.core.domain.{Player, PlayerWithInvite}
import fbariy.seafight.server.application.game.GameRepository
import fbariy.seafight.server.application.ship.ShipsRepository

import java.util.UUID

class CanPlayHandler[F[_]: FlatMap: Applicative: Concurrent](
    gameRepository: GameRepository[F],
    shipsRepository: ShipsRepository[F]) {
  def handle(inviteCtx: PlayerWithInvite): F[ValidatedNec[AppError, Unit]] =
    for {
      maybeGame <- gameRepository.findByIdAndPlayer(inviteCtx.invite.id,
                                                    inviteCtx.p)
      shipsValidators = (
        shipsAreSetup(inviteCtx.invite.id, inviteCtx.p),
        shipsAreSetup(inviteCtx.invite.id, inviteCtx.opp)
      )
      validEff           = ().validNec[AppError].pure[F]
      bothShipsValidator = shipsValidators.mapN(_ |+| _)

      bothShipsAreSetup <- maybeGame.fold(bothShipsValidator)(_ => validEff)
    } yield bothShipsAreSetup

  private def shipsAreSetup(inviteId: UUID,
                            p: Player): F[ValidatedNec[AppError, Unit]] =
    shipsRepository
      .has(inviteId, p)
      .map(
        isSetup =>
          if (isSetup) ().validNec[AppError]
          else ShipsAreNotSetupError(inviteId, p).invalidNec[Unit])
}
