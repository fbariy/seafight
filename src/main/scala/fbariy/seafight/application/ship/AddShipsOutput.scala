package fbariy.seafight.application.ship

import fbariy.seafight.application.invite.InviteOutput
import fbariy.seafight.domain.{Cell, Player}

case class AddShipsOutput(invite: InviteOutput, player: Player, ships: Seq[Cell])
