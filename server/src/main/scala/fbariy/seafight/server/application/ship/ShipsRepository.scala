package fbariy.seafight.server.application.ship

import fbariy.seafight.core.domain.{Cell, Invite, Player}

import java.util.UUID

case class PlayerShips(player: Player, ships: Seq[Cell])

trait ShipsRepository[F[_]] {
  def add(invite: Invite,
          p: Player,
          first: Boolean,
          ships: Seq[Cell]): F[Seq[Cell]]

  def release(invite: Invite): F[Option[(PlayerShips, PlayerShips)]]

  def has(inviteId: UUID, p: Player): F[Boolean]
}
