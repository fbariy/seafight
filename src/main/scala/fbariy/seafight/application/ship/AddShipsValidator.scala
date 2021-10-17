package fbariy.seafight.application.ship

import cats.Functor
import cats.data.ValidatedNec
import fbariy.seafight.application.error._
import fbariy.seafight.domain.Cell
import cats.implicits._
import fbariy.seafight.application.game.GameRepository

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
          if (gameOpt.isDefined) GameAlreadyExist.invalidNec[Unit]
          else ().validNec[GameAlreadyExist.type])
}
