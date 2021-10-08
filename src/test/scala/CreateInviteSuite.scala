import fbariy.seafight.application.invite.CreateInviteInput
import fbariy.seafight.domain.Player
import util.AppSuite

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class CreateInviteSuite extends AppSuite {
  test("You can't create a game where players are same") {
    for {
      ex.errFirst(err) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("  twaryna"), Player("twaryna  ")))
    } yield assertEquals(err.code, "PLAYERS_ARE_SAME")
  }

  test("You can't create a game where at least one of players is empty") {
    for {
      ex.errFirst(err) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("   "), Player("VooDooSh")))
    } yield assertEquals(err.code, "PLAYER_IS_EMPTY")
  }

  test("Create invite with trimmed players") {
    for {
      ex.suc(invite) -> _ <- appClient.createInvite(
        CreateInviteInput(Player("  VooDooSh "), Player("Stinger  ")))
    } yield {
      assertEquals(invite.player1, Player("VooDooSh"))
      assertEquals(invite.player2, Player("Stinger"))
    }
  }

  override def munitTimeout: Duration = new FiniteDuration(60, TimeUnit.SECONDS)
}
