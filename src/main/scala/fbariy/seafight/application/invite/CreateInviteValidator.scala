package fbariy.seafight.application.invite

import cats.data.ValidatedNec
import fbariy.seafight.application.errors.SamePlayersError
import fbariy.seafight.domain.Player
import cats.implicits._

class CreateInviteValidator {
  def playersAreNotSame(
      p1: Player,
      p2: Player): ValidatedNec[SamePlayersError.type, Player] =
    if (p1 == p2) SamePlayersError.invalidNec[Player]
    else p1.validNec[SamePlayersError.type]
}
