import fbariy.seafight.core.application.CreateInviteInput
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.{Cell, Player}
import util.AppSuite

import java.util.UUID

class CanPlaySuite extends AppSuite {
  fixtures.defaultGame.test("Both players can play on created game") { invite =>
    for {
      ex.suc(_) <- appClient.canPlay(invite.id, invite.player1)
      ex.suc(_) <- appClient.canPlay(invite.id, invite.player2)
    } yield ()
  }

  test("Player cannot play on not created invite") {
    for {
      ex.errFirst(error) <- appClient.canPlay(UUID.randomUUID(),
                                              Player("amieloo"))
    } yield assert(error.isInstanceOf[NotFoundInviteError.type])
  }

  test("Player can play when both players setup ships") {
    for {
      ex.suc(invite) <- appClient.createInvite(
        CreateInviteInput(Player("VooDooSh"), Player("amieloo")))

      ex.suc(_) <- appClient.addShips(invite.id, invite.player1)(
        Seq.empty[Cell]) //todo: сменить при реализации валидации

      ex.errFirst(error) <- appClient.canPlay(invite.id, invite.player1)
    } yield assert(error.isInstanceOf[ShipsAreNotSetupError])
  }
}
