package util

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import com.dimafeng.testcontainers.munit.TestContainerForAll
import fbariy.seafight.application.AppErrorOutput
import munit.CatsEffectSuite

trait AppSuite
    extends CatsEffectSuite
    with AppEnvironmentForAllTests
    with TestContainerForAll
    with AppDBSchemaAware
    with AppHttpClientAware {

  object ex {
    def unapply[A](validated: ValidatedNec[AppErrorOutput, A]): Some[A] = {
      validated match {
        case Valid(a) => Some(a)
        case Invalid(_) => fail(s"Response must be valid")
      }
    }
  }
}
