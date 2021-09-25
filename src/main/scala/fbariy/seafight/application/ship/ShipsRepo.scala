package fbariy.seafight.application.ship

import fbariy.seafight.domain.{Cell, Invite, Player}

case class PlayerShips(player: Player, ships: Seq[Cell])

trait ShipsRepo[F[_]] {
  def add(invite: Invite,
          p: Player,
          first: Boolean,
          ships: Seq[Cell]): F[Seq[Cell]]

  def release(invite: Invite): F[Option[(PlayerShips, PlayerShips)]]
}
