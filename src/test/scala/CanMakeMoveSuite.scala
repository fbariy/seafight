import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import fbariy.seafight.application.invite.CreateInviteInput
import fbariy.seafight.domain._
import fbariy.seafight.domain.Cell.CellOps
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
      assertEquals(clue(response.status), clue(NotFound))
      assertEquals(clue(error.code), clue("NOT_FOUND_GAME"))
    }
  }

  fixtures.newGame.test("Second player can't make a move on a new game") { invite =>
    for {
      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id,
        invite.player2)

    } yield assert(!canMakeMove)
  }

  fixtures.newGame.test("First player make a move on a new game") { invite =>
    for {
      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id,
                                                        invite.player1)
    } yield assert(canMakeMove)
  }

  fixtures.newGame.test(
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
