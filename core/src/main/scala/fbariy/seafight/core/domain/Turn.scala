package fbariy.seafight.core.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Turn(p: Player, kick: Cell, serial: Int)
object Turn {
  implicit val turnDecoder: Decoder[Turn] = deriveDecoder
  implicit val turnEncoder: Encoder[Turn] = deriveEncoder
}
