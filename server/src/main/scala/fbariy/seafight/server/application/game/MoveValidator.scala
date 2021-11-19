package fbariy.seafight.server.application.game

import cats.data.ValidatedNec
import cats.implicits._
import fbariy.seafight.core.application.error.{AppError, GameOverError, PlayerCannotMakeMoveError}
import fbariy.seafight.core.domain.{GameWithPlayers, PlayerWithGame}

class MoveValidator {
  def canMakeMove(played: PlayerWithGame): ValidatedNec[AppError, Unit] = {
    val canMakeMove = played match {
      case PlayerWithGame(p, _, _, GameWithPlayers(_, _, _, turns, p1, _, _)) =>
        turns.sortBy(-_.serial).headOption match {
          case Some(turn) => turn.p != p
          case None       => p1 == p
        }
    }

    if (canMakeMove) ().validNec[PlayerCannotMakeMoveError]
    else PlayerCannotMakeMoveError(played.p).invalidNec[Unit]
  }

  def gameIsNotOver(game: GameWithPlayers): ValidatedNec[AppError, Unit] =
    game.winner match {
      case Some(winner) => GameOverError(winner).invalidNec[Unit]
      case None => ().validNec[AppError]
    }
}
