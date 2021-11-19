import fbariy.seafight.core.domain.Digit._
import fbariy.seafight.core.domain.Symbol._
import fbariy.seafight.core.domain._
import org.http4s.Status.NotFound
import util.AppSuite

import java.util.UUID

class CanMakeMoveSuite extends AppSuite {
  test("Can't make a move on a not created game") {
    for {
      ex.errFirst(error) -> response <- appClient.canMakeMove(
        UUID.randomUUID(),
        Player("HellLighT"))
    } yield {
      assertEquals(clue(error.code), clue("NOT_FOUND_GAME"))
      assertEquals(clue(response.status), clue(NotFound))
    }
  }

  fixtures.defaultGame.test("Second player can't make a move on a new game") { invite =>
    for {
      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id,
        invite.player2)

    } yield assert(!canMakeMove)
  }

  fixtures.defaultGame.test("First player make a move on a new game") { invite =>
    for {
      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id,
                                                        invite.player1)
    } yield assert(canMakeMove)
  }

  fixtures.defaultGame.test(
    "Player can make a move if the last move was another player") { invite =>
    for {
      ex.suc(firstPlayerCanMakeFirstMove) -> _ <- appClient.canMakeMove(
        invite.id,
        invite.player1)
      _ <- appClient.move(invite.id, invite.player1)(A \ `9`)

      ex.suc(secondPlayerCanMakeSecondMove) -> _ <- appClient.canMakeMove(
        invite.id,
        invite.player2)

    } yield assert(firstPlayerCanMakeFirstMove && secondPlayerCanMakeSecondMove)
  }
}
