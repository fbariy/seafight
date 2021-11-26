import cats.implicits._
import fbariy.seafight.core.application.TurnOutput
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.Digit._
import fbariy.seafight.core.domain.Symbol._
import util.AppSuite

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class MoveSuite extends AppSuite {
  fixtures.defaultGame.test("Second player can't make a move on a new game") {
    invite =>
      for {
        ex.errFirst(err) <- appClient.move(invite.id, invite.player2)(A \ `1`)
      } yield assert(err.isInstanceOf[PlayerCannotMakeMoveError])
  }

  fixtures.defaultGame.test("Players must to make moves alternately") {
    invite =>
      for {
        _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
        ex.suc(gameAfterSecondMove) <- appClient.move(invite.id,
                                                      invite.player2)(A \ `2`)
      } yield {
        assertEquals(gameAfterSecondMove.player, invite.player2)
        assertEquals(gameAfterSecondMove.opponent, invite.player1)

        assertEquals(gameAfterSecondMove.playerTurns,
                     Seq(TurnOutput(A \ `2`, invite.player2, 2)))
        assertEquals(gameAfterSecondMove.opponentTurns,
                     Seq(TurnOutput(A \ `1`, invite.player1, 1)))
      }
  }

  fixtures.defaultGame.test("Player can't make a move twice") { invite =>
    for {
      _                <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.errFirst(err) <- appClient.move(invite.id, invite.player1)(B \ `2`)
    } yield assert(err.isInstanceOf[PlayerCannotMakeMoveError])
  }

  fixtures
    .game(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || @ | @ | @ | @ | ~ | @ | @ | @ | ~
        | 8 || @ | @ | @ | ~ | @ | @ | ~ | @ | @
        | 7 || @ | @ | ~ | @ | ~ | @ | ~ | @ | ~
        | 6 || @ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || . | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || . | . | . | . | . | . | . | . | .
        | 1 || . | . | . | . | . | . | . | . | .
        |""".stripMargin,
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || X | X | X | X | ~ | X | X | X | ~
        | 8 || X | X | X | ~ | X | X | ~ | X | X
        | 7 || X | X | ~ | X | ~ | X | ~ | X | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | @ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )
    .test("Player can't make a move on game that over") { invite =>
      for {
        ex.suc(gameAtLastMove) <- appClient.move(invite.id, invite.player1)(
          D \ `4`)
        ex.errFirst(err) <- appClient.move(invite.id, invite.player2)(A \ `1`)
      } yield {
        assertEquals(clue(gameAtLastMove.winner), clue(Some(invite.player1)))
        assert(err.isInstanceOf[GameOverError])
      }
    }

  fixtures.defaultGame.test(
    "When trying to make moves concurrently by the player, only one will be successful") {
    invite =>
      for {
        seqValidated <- Seq(
          appClient.move(invite.id, invite.player1)(A \ `1`),
          appClient.move(invite.id, invite.player1)(A \ `2`),
          appClient.move(invite.id, invite.player1)(A \ `3`),
        ).parSequence
      } yield assertEquals(seqValidated.count(_.isValid), 1)
  }

  fixtures.defaultGame.test(
    "Player can't make a move when back to move requested") { invite =>
    for {
      _                <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.suc(_)        <- appClient.backToMove(invite.id, invite.player1)(1)
      ex.errFirst(err) <- appClient.move(invite.id, invite.player1)(I \ `9`)
    } yield assert(err.isInstanceOf[BackAlreadyRequestedError.type])
  }

  override def munitTimeout: Duration =
    new FiniteDuration(240, TimeUnit.SECONDS)
}
