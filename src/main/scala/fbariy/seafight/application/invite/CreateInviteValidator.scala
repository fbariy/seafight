package fbariy.seafight.application.invite

import cats.data.ValidatedNec
import fbariy.seafight.application.errors.{AppError, EmptyPlayerError, SamePlayersError}
import fbariy.seafight.domain.Player
import cats.implicits._

class CreateInviteValidator {
  def playerIsNotEmpty(p: Player): ValidatedNec[AppError, Unit] =
    if (p.name.trim.isEmpty) EmptyPlayerError.invalidNec[Unit]
    else ().validNec[EmptyPlayerError.type]

  def playersAreNotSame(
      p1: Player,
      p2: Player): ValidatedNec[AppError, Unit] =
    if (p1 == p2) SamePlayersError.invalidNec[Unit]
    else ().validNec[SamePlayersError.type]
}
