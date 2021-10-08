package fbariy.seafight.application

import fbariy.seafight.domain.{Cell, Player}

object errors {
  sealed trait AppError

  sealed trait AuthError                        extends AppError
  case object MissingGameIdError                extends AuthError
  case object MissingPlayerError                extends AuthError
  case object MissingInviteIdError              extends AuthError
  case class BadFormatGameIdError(id: String)   extends AuthError
  case class BadFormatInviteIdError(id: String) extends AuthError
  case object NotFoundGameError                 extends AuthError
  case object NotFoundInviteError               extends AuthError

  case class NotCorrectShipsError(ships: Seq[Cell]) extends AppError
  case object SamePlayersError                      extends AppError
  case class PlayerCannotMakeMoveError(p: Player)   extends AppError
  case class GameOverError(winner: Player)          extends AppError
  case object EmptyPlayerError                      extends AppError
}
