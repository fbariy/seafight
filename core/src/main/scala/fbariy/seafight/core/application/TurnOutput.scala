package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Cell, Player, Turn}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class TurnOutput(kick: Cell, player: Player, serial: Int)
object TurnOutput {
  def apply(turn: Turn): TurnOutput = TurnOutput(turn.kick, turn.p, turn.serial)

  implicit val turnOutputEncoder: Encoder[TurnOutput] = deriveEncoder
  implicit val turnOutputDecoder: Decoder[TurnOutput] = deriveDecoder
}
