package fbariy.seafight.application.invite

import fbariy.seafight.domain.{Invite, Player}

import java.util.UUID
import scala.language.implicitConversions

case class InviteOutput(id: UUID, player1: Player, player2: Player)
object InviteOutput {
  implicit def entityToOutput(invite: Invite): InviteOutput =
    InviteOutput(invite.id, invite.p1, invite.p2)
}
