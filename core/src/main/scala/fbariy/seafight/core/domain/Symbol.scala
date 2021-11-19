package fbariy.seafight.core.domain

import enumeratum._

sealed trait Symbol extends EnumEntry

case object Symbol extends Enum[Symbol] with CirceEnum[Symbol] {
  case object A extends Symbol
  case object B extends Symbol
  case object C extends Symbol
  case object D extends Symbol
  case object E extends Symbol
  case object F extends Symbol
  case object G extends Symbol
  case object H extends Symbol
  case object I extends Symbol

  val values: IndexedSeq[Symbol] = findValues

  implicit class SymbolOps(symbol: Symbol) {
    def \(digit: Digit): Cell = Cell(symbol, digit)
  }
}
