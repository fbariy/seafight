package fbariy.seafight.application.game

import cats.data.ValidatedNec
import cats.implicits._
import fbariy.seafight.application.errors.{
  AppError,
  GameOverError,
  PlayerCannotMakeMoveError
}
import fbariy.seafight.domain.{Cell, GameWithPlayers}
import fbariy.seafight.infrastructure.PlayerWithGame

class MoveValidator(canMakeMoveHandler: CanMakeMoveHandler) {
  def canMakeMove(played: PlayerWithGame): ValidatedNec[AppError, Unit] =
    if (canMakeMoveHandler.handle(played))
      ().validNec[PlayerCannotMakeMoveError]
    else PlayerCannotMakeMoveError(played.p).invalidNec[Unit]

  def gameIsNotOver(game: GameWithPlayers): ValidatedNec[AppError, Unit] =
    if (checkGameOver(game.p2Ships, game.getPlayerTurns(game.p1).map(_.kick)))
      GameOverError(game.p1)
        .invalidNec[Unit]
    else if (checkGameOver(game.p1Ships,
                           game.getPlayerTurns(game.p2).map(_.kick)))
      GameOverError(game.p2)
        .invalidNec[Unit]
    else ().validNec[GameOverError]

  private def checkGameOver(ships: Seq[Cell], moves: Seq[Cell]): Boolean =
    if (moves.size < 20) false
    else (moves intersect ships).size >= 20
}
