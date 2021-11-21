import fbariy.seafight.core.domain.Player
import fbariy.seafight.server.infrastructure.client.PlayerState
import util.AppSuite
import cats.implicits._
import fbariy.seafight.core.application
import fbariy.seafight.core.application.CreateInviteInput

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
      ex.suc(invite) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      ex.errFirst(err1) -> _ <- appClient.addShipsThrowable(invite.id, invite.player1)(
        state1.getOrElse(fail("state must be valid")).ships)
      ex.errFirst(err2) -> _ <- appClient.addShipsThrowable(invite.id, invite.player1)(
        state2.getOrElse(fail("state must be valid")).ships)
    } yield {
      assertEquals(err1.code, "NOT_CORRECT_SHIPS")
      assertEquals(err2.code, "NOT_CORRECT_SHIPS")
    }
  }

  test("Player can to add ships") {
    for {
      ex.suc(invite) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      ex.suc(output) -> _ <- appClient.addShipsThrowable(invite.id, invite.player1)(
        state.ships)
    } yield assertEquals(output.ships, state.ships)
  }

  fixtures.defaultGame.test("Player can't add ships to an existing game") {
    invite =>
      val state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      for {
        ex.errFirst(err) -> _ <- appClient.addShipsThrowable(invite.id, invite.player1)(
          state.ships)
      } yield assertEquals(err.code, "GAME_ALREADY_EXIST")
  }

  test(
    "When trying add ships to another player concurrently, only one will be successfully") {
    for {
      ex.suc(invite) -> _ <- appClient.createInviteThrowable(
        application.CreateInviteInput(Player("beZZdar"), Player("Hoho")))

      state = PlayerState.fromString.getOrElse(fail("state must be valid"))

      _ <- appClient.addShipsThrowable(invite.id, invite.player1)(state.ships)

      addShipsAction = appClient.addShipsThrowable(invite.id, invite.player2)(
        state.ships)

      seqValidated <- Seq(addShipsAction, addShipsAction, addShipsAction).parSequence
    } yield assertEquals(seqValidated.count(_._1.isValid), 1)
  }
}
