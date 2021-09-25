package fbariy.seafight.domain

sealed trait Digit
case object One extends Digit
case object Two extends Digit
case object Three extends Digit
case object Four extends Digit
case object Five extends Digit
case object Six extends Digit
case object Seven extends Digit
case object Eight extends Digit
case object Nine extends Digit

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

case class Cell(digit: Digit, symbol: Symbol)
