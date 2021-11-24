import fbariy.seafight.core.application.CreateInviteInput
import fbariy.seafight.core.domain.Player
import util.AppSuite

import java.util.UUID

class GetInviteSuite extends AppSuite {
  test("You can't get not-existent invite") {
    for {
      ex.errFirst(error) <- appClient.getInvite(UUID.randomUUID(),
                                                Player("unnamed"))
    } yield assertEquals(error.code, "NOT_FOUND_INVITE")
  }

  test("You can get existent invite") {
    for {
      ex.suc(createdInvite) <- appClient.createInvite(
        CreateInviteInput(Player("King_SPB"), Player("VooDooSh")))
      ex.suc(obtainedInvite) <- appClient.getInvite(createdInvite.id,
                                                    Player("VooDooSh"))
    } yield {
      assertEquals(obtainedInvite.id, createdInvite.id)
      assertEquals(obtainedInvite.player1, createdInvite.player1)
      assertEquals(obtainedInvite.player2, createdInvite.player2)
    }
  }
}
