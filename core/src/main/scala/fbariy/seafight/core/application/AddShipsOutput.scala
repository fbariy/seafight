package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Cell, Player}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AddShipsOutput(invite: InviteOutput,
                          player: Player,
                          ships: Seq[Cell])
object AddShipsOutput {
  implicit val addShipsOutputEncoder: Encoder[AddShipsOutput] = deriveEncoder
  implicit val addShipsOutputDecoder: Decoder[AddShipsOutput] = deriveDecoder
}
