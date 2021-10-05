package fbariy.seafight.domain

import enumeratum._

sealed trait Digit extends EnumEntry

case object Digit extends Enum[Digit] with CirceEnum[Digit] {
  case object `1` extends Digit
  case object `2` extends Digit
  case object `3` extends Digit
  case object `4` extends Digit
  case object `5` extends Digit
  case object `6` extends Digit
  case object `7` extends Digit
  case object `8` extends Digit
  case object `9` extends Digit

  val values: IndexedSeq[Digit] = findValues
}
