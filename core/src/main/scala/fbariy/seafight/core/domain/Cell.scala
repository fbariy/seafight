package fbariy.seafight.core.domain

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class Cell(symbol: Symbol, digit: Digit)
object Cell {
  implicit val cellDecoder: Decoder[Cell] = deriveDecoder
  implicit val cellEncoder: Encoder[Cell] = deriveEncoder
}
