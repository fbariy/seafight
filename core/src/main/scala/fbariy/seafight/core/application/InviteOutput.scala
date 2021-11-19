package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Invite, Player, PlayerWithInvite}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID
import scala.language.implicitConversions

case class InviteOutput(id: UUID, player1: Player, player2: Player)
object InviteOutput {
  def apply(invite: Invite): InviteOutput =
    InviteOutput(invite.id, invite.p1, invite.p2)

  implicit class InviteOutputOps(output: InviteOutput) {
    def toInvite: Invite = Invite(output.id, output.player1, output.player2)

    def toPlayerContext(isFirstPlayer: Boolean): PlayerWithInvite =
      if (isFirstPlayer)
        PlayerWithInvite(output.player1,
                         output.player2,
                         isFirstPlayer,
                         toInvite)
      else
        PlayerWithInvite(output.player2,
                         output.player1,
                         isFirstPlayer,
                         toInvite)
  }

  implicit val inviteOutputEncoder: Encoder[InviteOutput] = deriveEncoder
  implicit val inviteOutputDecoder: Decoder[InviteOutput] = deriveDecoder
}
