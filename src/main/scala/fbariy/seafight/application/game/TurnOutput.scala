package fbariy.seafight.application.game

import fbariy.seafight.domain.{Cell, Player, Turn}

case class TurnOutput(kick: Cell, player: Player, serial: Int)
object TurnOutput {
  def apply(turn: Turn): TurnOutput = TurnOutput(turn.kick, turn.p, turn.serial)
}
