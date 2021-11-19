package fbariy.seafight.core.application

import fbariy.seafight.core.domain.Player
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class CreateInviteInput(player1: Player, player2: Player)
object CreateInviteInput {
  implicit val createInviteInputDecoder: Decoder[CreateInviteInput] =
    deriveDecoder
  implicit val createInviteInputEncoder: Encoder[CreateInviteInput] =
    deriveEncoder
}
