package fbariy.seafight.core.infrastructure

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.validated._
import cats.syntax.apply._

object codec {
  implicit def validatedNecEncoder[E: Encoder, A: Encoder]
    : Encoder[ValidatedNec[E, A]] = {
    case Valid(a) => Encoder[A].apply(a)
    case Invalid(e) =>
      Json.obj(
        "type"   -> Json.fromString("error"),
        "errors" -> Encoder[NonEmptyChain[E]].apply(e)
      )
  }

  implicit def validatedNecDecoder[E: Decoder, A: Decoder]
    : Decoder[ValidatedNec[E, A]] =
    (c: HCursor) =>
      (c.get[String]("type"), c.get[Json]("errors")).tupled match {
        case Left(_) => Decoder[A].apply(c).map(_.valid.toValidatedNec)
        case Right(_ -> errors) =>
          Decoder[NonEmptyChain[E]].decodeJson(errors).map(_.invalid)
    }
}
