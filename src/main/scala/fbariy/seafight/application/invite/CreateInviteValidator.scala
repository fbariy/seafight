package fbariy.seafight.application.invite

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import fbariy.seafight.application.errors.SamePlayersError
import fbariy.seafight.domain.Player

class CreateInviteValidator {
  def playersAreNotSame(
      p1: Player,
      p2: Player): ValidatedNec[SamePlayersError.type, Player] =
    if (p1 == p2) Invalid(SamePlayersError).toValidatedNec
    else Valid(p1).toValidatedNec
}
