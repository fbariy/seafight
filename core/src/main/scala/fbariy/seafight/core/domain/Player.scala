package fbariy.seafight.core.domain

import io.circe.{Decoder, Encoder, HCursor, Json}

case class Player(name: String) extends AnyVal
object Player {
  implicit val playerDecoder: Decoder[Player] =
    (c: HCursor) => c.as[String].map(Player(_))

  implicit val playerEncoder: Encoder[Player] =
    (a: Player) => Json.fromString(a.name)
}
