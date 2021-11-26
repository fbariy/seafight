import fbariy.seafight.core.application
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.Player
import util.AppSuite

class CreateInviteSuite extends AppSuite {
  test("You can't create a game where players are same") {
    for {
      ex.errFirst(err) <- appClient.createInvite(
        application.CreateInviteInput(Player("  twaryna"), Player("twaryna  ")))
    } yield assert(err.isInstanceOf[SamePlayersError.type])
  }

  test("You can't create a game where at least one of players is empty") {
    for {
      ex.errFirst(err) <- appClient.createInvite(
        application.CreateInviteInput(Player("   "), Player("VooDooSh")))
    } yield assert(err.isInstanceOf[EmptyPlayerError.type])
  }

  test("Create invite with trimmed players") {
    for {
      ex.suc(invite) <- appClient.createInvite(
        application.CreateInviteInput(Player("  VooDooSh "),
                                      Player("Stinger  ")))
    } yield {
      assertEquals(invite.player1, Player("VooDooSh"))
      assertEquals(invite.player2, Player("Stinger"))
    }
  }
}
