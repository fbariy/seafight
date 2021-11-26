import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.Digit._
import fbariy.seafight.core.domain.Symbol._
import fbariy.seafight.core.domain._
import util.AppSuite

import java.util.UUID

class CanMakeMoveSuite extends AppSuite {
  test("Can't make a move on a not created game") {
    for {
      ex.errFirst(error) <- appClient.canMakeMove(UUID.randomUUID(),
                                                  Player("HellLighT"))
    } yield assert(error.isInstanceOf[NotFoundGameError.type])
  }

  fixtures.defaultGame.test("Second player can't make a move on a new game") {
    invite =>
      for {
        ex.suc(canMakeMove) <- appClient.canMakeMove(invite.id, invite.player2)

      } yield assert(!canMakeMove)
  }

  fixtures.defaultGame.test("First player make a move on a new game") {
    invite =>
      for {
        ex.suc(canMakeMove) <- appClient.canMakeMove(invite.id, invite.player1)
      } yield assert(canMakeMove)
  }

  fixtures.defaultGame.test(
    "Player can make a move if the last move was another player") { invite =>
    for {
      ex.suc(firstPlayerCanMakeFirstMove) <- appClient.canMakeMove(
        invite.id,
        invite.player1)
      _ <- appClient.move(invite.id, invite.player1)(A \ `9`)

      ex.suc(secondPlayerCanMakeSecondMove) <- appClient.canMakeMove(
        invite.id,
        invite.player2)

    } yield assert(firstPlayerCanMakeFirstMove && secondPlayerCanMakeSecondMove)
  }
}
