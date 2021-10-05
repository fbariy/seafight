package util

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.effect.{IO, Resource, SyncIO}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.domain.Player
import munit.CatsEffectSuite
import cats.implicits._

trait AppSuite
    extends CatsEffectSuite
    with AppEnvironmentForAllTests
    with TestContainerForAll
    with AppDBSchemaAware
    with AppHttpClientAware {

  object ex {
    object suc {
      def unapply[A](validated: ValidatedNec[AppErrorOutput, A]): Some[A] =
        validated match {
          case Valid(a)   => Some(a)
          case Invalid(_) => fail(s"Response must be valid")
        }
    }

    object errFirst {
      def unapply[A](
          validated: ValidatedNec[AppErrorOutput, A]): Some[AppErrorOutput] =
        Some(err.unapply(validated).value.head)
    }

    object err {
      def unapply[A](validated: ValidatedNec[AppErrorOutput, A])
        : Some[NonEmptyChain[AppErrorOutput]] =
        validated match {
          case Valid(_)        => fail(s"Response must be invalid")
          case Invalid(errors) => Some(errors)
        }
    }
  }

  object fixtures {
    val newGame: SyncIO[FunFixture[InviteOutput]] = ResourceFixture(
      Resource.eval {
        for {
          appClient <- IO.delay(appClient)

          ex.suc(invite) -> _ <- appClient.createInvite(
            CreateInviteInput(Player("VooDooSh"), Player("twaryna")))

          _ <- (
            //todo: после ввода валидации заполнить корабли
            appClient.addShips(invite.id, Player("twaryna"))(Seq.empty),
            appClient.addShips(invite.id, Player("VooDooSh"))(Seq.empty)
          ).tupled

        } yield invite
      })
  }
}
