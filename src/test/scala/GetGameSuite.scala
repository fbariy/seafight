import fbariy.seafight.domain.Cell.CellOps
import fbariy.seafight.domain.Digit.`4`
import fbariy.seafight.domain.Symbol.D
import fbariy.seafight.infrastructure.client.PlayerState
import util.AppSuite

class GetGameSuite extends AppSuite {
  private val rawState1: String =
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
      |""".stripMargin
  private val rawState2: String =
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

  fixtures.game(rawState1, rawState2).test("Returns actual game state") { invite =>
    val state1 =
      PlayerState.fromString(rawState1).getOrElse(fail("state must be valid"))
    val state2 =
      PlayerState.fromString(rawState2).getOrElse(fail("state must be valid"))

    for {
      ex.suc(p1Game) -> _ <- appClient.getGame(invite.id, invite.player1)
      ex.suc(p2Game) -> _ <- appClient.getGame(invite.id, invite.player2)

      _ <- appClient.move(invite.id, invite.player1)(D \ `4`)

      ex.suc(gameAfterOver) -> _ <- appClient.getGame(invite.id, invite.player1)
    } yield {
      assertEquals(p1Game.ships.toSet, state1.ships.toSet)
      assertEquals(p1Game.winner, None)
      assertEquals(p1Game.player, invite.player1)
      assertEquals(p1Game.opponent, invite.player2)
      assertEquals(p1Game.playerTurns.map(_.kick).toSet, state2.kicks.toSet)
      assertEquals(p1Game.opponentTurns.map(_.kick).toSet, state1.kicks.toSet)

      assertEquals(p2Game.ships.toSet, state2.ships.toSet)
      assertEquals(p2Game.winner, None)
      assertEquals(p2Game.player, invite.player2)
      assertEquals(p2Game.opponent, invite.player1)
      assertEquals(p2Game.playerTurns.map(_.kick).toSet, state1.kicks.toSet)
      assertEquals(p2Game.opponentTurns.map(_.kick).toSet, state2.kicks.toSet)

      assertEquals(gameAfterOver.winner, Some(invite.player1))
    }
  }
}
