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

class MoveValidator {
  def canMakeMove(played: PlayerWithGame): ValidatedNec[AppError, Unit] = {
    val canMakeMove = played match {
      case PlayerWithGame(p, _, _, GameWithPlayers(_, _, _, turns, p1, _)) =>
        turns.sortBy(-_.serial).headOption match {
          case Some(turn) => turn.p != p
          case None       => p1 == p
        }
    }

    if (canMakeMove) ().validNec[PlayerCannotMakeMoveError]
    else PlayerCannotMakeMoveError(played.p).invalidNec[Unit]
  }

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
