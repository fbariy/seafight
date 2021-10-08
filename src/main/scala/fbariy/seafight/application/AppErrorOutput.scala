package fbariy.seafight.application

import cats.Show

case class AppErrorOutput(code: String, message: String)
object AppErrorOutput {
  implicit val show: Show[AppErrorOutput] = (t: AppErrorOutput) =>
    s"Code: ${t.code}. Message: ${t.message}"
}
