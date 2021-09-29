package fbariy.seafight.domain

import scala.language.postfixOps

sealed trait Digit
case object `1` extends Digit
case object `2` extends Digit
case object `3` extends Digit
case object `4` extends Digit
case object `5` extends Digit
case object `6` extends Digit
case object `7` extends Digit
case object `8` extends Digit
case object `9` extends Digit

sealed trait Symbol
case object A extends Symbol
case object B extends Symbol
case object C extends Symbol
case object D extends Symbol
case object E extends Symbol
case object F extends Symbol
case object G extends Symbol
case object H extends Symbol
case object I extends Symbol

case class Cell(symbol: Symbol, digit: Digit)
object Cell {
  implicit class CellOps(symbol: Symbol) {
    def \(digit: Digit): Cell = Cell(symbol, digit)
  }
}
