import fbariy.seafight.core.domain.Player
import fbariy.seafight.server.infrastructure.client.PlayerState
import util.AppSuite
import cats.implicits._
import fbariy.seafight.core.application
import fbariy.seafight.core.application.CreateInviteInput
import fbariy.seafight.core.application.error._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class AddShipsSuite extends AppSuite {
  override def munitTimeout: Duration = new FiniteDuration(60, TimeUnit.SECONDS)

  test("Ships must be placed valid".ignore) {
    val state1 = PlayerState
      .fromString(
        """   || A | B | C | D | E | F | G | H | I
          |========================================
          | 9 || @ | @ | @ | @ | ~ | @ | @ | @ | ~
          | 8 || @ | @ | @ | ~ | @ | @ | ~ | @ | @
          | 7 || @ | @ | ~ | @ | ~ | @ | ~ | @ | ~
          | 6 || @ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          |""".stripMargin
      )
    val state2 = PlayerState
      .fromString(
        """   || A | B | C | D | E | F | G | H | I
          |========================================
          | 9 || @ | @ | @ | @ | ~ | @ | @ | @ | ~
          | 8 || @ | @ | @ | ~ | @ | @ | ~ | @ | @
          | 7 || @ | @ | ~ | @ | ~ | @ | ~ | @ | ~
          | 6 || @ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
          |""".stripMargin
      )

    for {
      ex.suc(invite) <- appClient.createInvite(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      ex.errFirst(err1) <- appClient.addShips(invite.id, invite.player1)(
        state1.getOrElse(fail("state must be valid")).ships)
      ex.errFirst(err2) <- appClient.addShips(invite.id, invite.player1)(
        state2.getOrElse(fail("state must be valid")).ships)
    } yield {
      assert(err1.isInstanceOf[NotCorrectShipsError])
      assert(err2.isInstanceOf[NotCorrectShipsError])
    }
  }

  test("Player can to add ships") {
    for {
      ex.suc(invite) <- appClient.createInvite(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      ex.suc(output) <- appClient.addShips(invite.id, invite.player1)(
        state.ships)
    } yield assertEquals(output.ships, state.ships)
  }

  fixtures.defaultGame.test("Player can't add ships to an existing game") {
    invite =>
      val state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      for {
        ex.errFirst(err) <- appClient.addShips(invite.id, invite.player1)(
          state.ships)
      } yield assert(err.isInstanceOf[GameAlreadyExistError.type])
  }

  test(
    "When trying add ships to another player concurrently, only one will be successfully") {
    for {
      ex.suc(invite) <- appClient.createInvite(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      _ <- appClient.addShips(invite.id, invite.player1)(state.ships)

      addShipsAction = appClient.addShips(invite.id, invite.player2)(
        state.ships)

      seqValidated <- Seq(addShipsAction, addShipsAction, addShipsAction).parSequence
    } yield assertEquals(seqValidated.count(_.isValid), 1)
  }
}
