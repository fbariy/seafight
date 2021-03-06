package fbariy.seafight.server.application.back

import cats.Functor
import cats.data.ValidatedNec
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.{GameWithPlayers, Player, Turn}
import cats.syntax.validated._
import cats.syntax.functor._
import cats.syntax.either._

class BackToMoveValidator[F[_]: Functor](repository: BackToMoveRepository[F]) {
  def moveIsExist(game: GameWithPlayers,
                  moveNumber: Int): ValidatedNec[AppError, Turn] =
    game.turns.find(_.serial == moveNumber) match {
      case Some(turn) => turn.validNec[AppError]
      case None       => MoveIsNotExistError.invalidNec[Turn]
    }

  def backIsNotRequested(
      game: GameWithPlayers): F[ValidatedNec[AppError, Unit]] =
    repository
      .find(game.id)
      .map { opt =>
        if (opt.isDefined) BackAlreadyRequestedError.invalidNec[Unit]
        else ().validNec[BackAlreadyRequestedError.type]
      }

  def backRequested(
      game: GameWithPlayers): F[ValidatedNec[AppError, (Player, Turn)]] =
    repository
      .find(game.id)
      .map(_.toRight(BackNotRequestedError).toValidatedNec)

  def isOpponentAccepts(opp: Player, backInitiator: Player): ValidatedNec[AppError, Unit] =
    if (opp == backInitiator) InitiatorCannotAcceptBackError.invalidNec[Unit]
    else ().validNec[InitiatorCannotAcceptBackError.type]
}
