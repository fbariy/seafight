package fbariy.seafight.application.ship

import cats.data.Validated.Valid
import cats.data.ValidatedNec
import fbariy.seafight.application.errors._
import fbariy.seafight.domain.Cell

class AddShipsValidator {
  def correctedShips(ships: Seq[Cell]): ValidatedNec[NotCorrectShipsError, Seq[Cell]] =
  //todo: implements it
    Valid(ships).toValidatedNec
}

