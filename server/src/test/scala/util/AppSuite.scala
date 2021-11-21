package util

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.effect.{IO, Resource, Sync, SyncIO}
import cats.implicits._
import com.dimafeng.testcontainers.munit.TestContainerForAll
import fbariy.seafight.core.application.{AppErrorOutput, CreateInviteInput, InviteOutput}
import fbariy.seafight.core.domain.Player
import fbariy.seafight.server.infrastructure.client.PlayerState
import fbariy.seafight.core.infrastructure.SeafightClient
import munit.CatsEffectSuite

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
          case Invalid(e) => fail(s"Response must be valid. Cause: ${e.show}")
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
    val defaultGame: SyncIO[FunFixture[InviteOutput]] =
      game(PlayerState.defaultState, PlayerState.defaultState)

    def game(p1RawState: String,
             p2RawState: String): SyncIO[FunFixture[InviteOutput]] =
      ResourceFixture(Resource.eval {
        for {
          appClient <- IO.delay(appClient)
          invite    <- applyPlayerStates(p1RawState, p2RawState, appClient)
        } yield invite
      })

    private def applyPlayerStates[F[_]: Sync](
        p1RawState: String,
        p2RawState: String,
        appClient: SeafightClient[F]): F[InviteOutput] =
      for {
        playerStatesResult <- Sync[F].delay(
          (PlayerState.fromString(p1RawState),
           PlayerState
             .fromString(p2RawState)).tupled)

        p1State -> p2State = playerStatesResult match {
          case Left(error) =>
            fail(s"Player states format are wrong. Cause: $error")
          case Right(states) => states
        }

        ex.suc(invite) -> _ <- appClient.createInviteThrowable(
          CreateInviteInput(Player("VooDooSh"), Player("twaryna")))

        _ <- (
          appClient.addShipsThrowable(invite.id, Player("VooDooSh"))(p1State.ships),
          appClient.addShipsThrowable(invite.id, Player("twaryna"))(p2State.ships),
        ).tupled

        _ <- combineThroughOne(
          p2State.kicks.map(appClient.move(invite.id, invite.player1)(_)),
          p1State.kicks.map(appClient.move(invite.id, invite.player2)(_))
        ).sequence

      } yield invite

    private def combineThroughOne[T](seq1: Seq[T], seq2: Seq[T]): Seq[T] =
      seq1.zipWithIndex.flatMap {
        case item -> i => seq2.lift(i).map(Seq(item, _)).getOrElse(Seq(item))
      }
  }
}
