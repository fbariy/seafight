import fbariy.seafight.core.application
import fbariy.seafight.core.application.CreateInviteInput
import fbariy.seafight.core.domain.Player
import util.AppSuite

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class CreateInviteSuite extends AppSuite {
  test("You can't create a game where players are same") {
    for {
      ex.errFirst(err) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("  twaryna"), Player("twaryna  ")))
    } yield assertEquals(err.code, "PLAYERS_ARE_SAME")
  }

  test("You can't create a game where at least one of players is empty") {
    for {
      ex.errFirst(err) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("   "), Player("VooDooSh")))
    } yield assertEquals(err.code, "PLAYER_IS_EMPTY")
  }

  test("Create invite with trimmed players") {
    for {
      ex.suc(invite) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("  VooDooSh "), Player("Stinger  ")))
    } yield {
      assertEquals(invite.player1, Player("VooDooSh"))
      assertEquals(invite.player2, Player("Stinger"))
    }
  }
}
