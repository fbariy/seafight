import fbariy.seafight.domain._
import util.AppSuite
import cats.implicits._
import fbariy.seafight.application.game.TurnOutput
import fbariy.seafight.domain.Cell.CellOps

class MoveSuite extends AppSuite {
  fixtures.newGame.test("Second player can't make a move on a new game") {
    invite =>
      for {
        ex.errFirst(err) -> _ <- appClient.move(invite.id, invite.player2)(
          A \ `1`)
      } yield assert(err.code == "PLAYER_CANNOT_MAKE_MOVE")
  }

  fixtures.newGame.test("Players can make moves alternately") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.suc(gameAfterSecondMove) -> _ <- appClient.move(
        invite.id,
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

  fixtures.newGame.test("Player can't make a move twice") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.errFirst(err) -> _ <- appClient.move(invite.id, invite.player1)(
        B \ `2`)
    } yield assert(err.code == "PLAYER_CANNOT_MAKE_MOVE")
  }

  fixtures.newGame.test("Player can't a move on game that over".ignore) {
    invite =>
      ()
  }

  fixtures.newGame.test(
    "When trying to make moves concurrently by the player, only one will be successful".ignore) {
    invite =>
      for {
        seqValidated <- Seq(
          appClient.move(invite.id, invite.player1)(A \ `1`),
          appClient.move(invite.id, invite.player1)(A \ `2`),
          appClient.move(invite.id, invite.player1)(A \ `3`),
        ).parSequence
      } yield assert(clue(seqValidated.count(_._1.isValid)) == clue(1))
  }
}
