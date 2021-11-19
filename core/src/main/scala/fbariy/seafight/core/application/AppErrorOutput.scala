package fbariy.seafight.core.application

import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AppErrorOutput(code: String, message: String)
object AppErrorOutput {
  implicit val show: Show[AppErrorOutput] = (t: AppErrorOutput) => t.message

  implicit val appErrorOutputDecoder: Decoder[AppErrorOutput] = deriveDecoder
  implicit val appErrorOutputEncoder: Encoder[AppErrorOutput] = deriveEncoder
}
