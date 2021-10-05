package fbariy.seafight.domain

import scala.language.postfixOps

case class Cell(symbol: Symbol, digit: Digit)
object Cell {
  implicit class CellOps(symbol: Symbol) {
    def \(digit: Digit): Cell = Cell(symbol, digit)
  }
}
