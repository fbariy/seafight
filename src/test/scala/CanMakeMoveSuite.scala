import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import fbariy.seafight.application.invite.CreateInviteInput
import fbariy.seafight.domain.Player
import org.http4s.Status.NotFound
import util.AppSuite

import java.util.UUID

class CanMakeMoveSuite extends AppSuite {
  test("Can't make a move on a not created game") {
    for {
      validated -> response <- appClient.canMakeMove(UUID.randomUUID(),
                                                     Player("HellLighT"))
    } yield {
      assertEquals(clue(response.status), clue(NotFound))

      validated match {
        case Valid(_) => fail("response must be invalid")
        case Invalid(errors) =>
          assertEquals(clue(errors.head.code), clue("NOT_FOUND_GAME"))
      }
    }
  }

  test("Second player can't make a move on a new game") {
    for {
      ex.suc(invite) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("VooDooSh"), Player("twaryna")))

      _ <- (
        appClient.addShips(invite.id, Player("twaryna"))(Seq.empty),
        appClient.addShips(invite.id, Player("VooDooSh"))(Seq.empty)
      ).tupled

      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id, invite.player2)

    } yield assert(!canMakeMove)
  }

  test("First player make a move on a new game") {
    for {
      ex.suc(invite) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("VooDooSh"), Player("twaryna")))

      _ <- (
        appClient.addShips(invite.id, Player("twaryna"))(Seq.empty),
        appClient.addShips(invite.id, Player("VooDooSh"))(Seq.empty)
      ).tupled

      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id, invite.player1)

    } yield assert(canMakeMove)
  }

  test("Player can make a move if the last move was another player".ignore) {
    //todo: сделать ход первым игроком и убедиться что второй игрок может сделать ход
    for {
      ex.suc(invite) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("VooDooSh"), Player("twaryna")))

      _ <- (
        appClient.addShips(invite.id, Player("twaryna"))(Seq.empty),
        appClient.addShips(invite.id, Player("VooDooSh"))(Seq.empty)
      ).tupled

      ex.suc(canMakeMove) -> _ <- appClient.canMakeMove(invite.id, invite.player1)

    } yield assert(canMakeMove)
  }
}
