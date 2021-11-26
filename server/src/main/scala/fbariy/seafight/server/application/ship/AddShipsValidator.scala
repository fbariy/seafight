package fbariy.seafight.server.application.ship

import cats.Functor
import cats.data.ValidatedNec
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.Cell
import cats.implicits._
import fbariy.seafight.server.application.game.GameRepository

import java.util.UUID

class AddShipsValidator[F[_]: Functor](gameRepository: GameRepository[F]) {
  def correctedShips(ships: Seq[Cell]): ValidatedNec[AppError, Unit] =
    //todo: implements it
    ().validNec[NotCorrectShipsError]

  def gameIsNotCreated(inviteId: UUID): F[ValidatedNec[AppError, Unit]] =
    gameRepository
      .find(inviteId)
      .map(
        gameOpt =>
          if (gameOpt.isDefined) GameAlreadyExistError.invalidNec[Unit]
          else ().validNec[GameAlreadyExistError.type])
}
