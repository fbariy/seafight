import fbariy.seafight.domain.Digit._
import fbariy.seafight.domain.Symbol._
import util.AppSuite
import cats.implicits._

class BackToMoveSuite extends AppSuite {
  fixtures.defaultGame.test("Specified move must be exist") { invite =>
    for {
      ex.errFirst(err) -> _ <- appClient.backToMove(invite.id, invite.player1)(
        1000)
    } yield assertEquals(err.code, "MOVE_IS_NOT_EXIST")
  }

  fixtures.defaultGame.test("Only one back can be requested") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      ex.suc(_) -> _ <- appClient.backToMove(invite.id, invite.player1)(1)
      ex.errFirst(err) -> _ <- appClient.backToMove(invite.id, invite.player1)(
        1)
    } yield assertEquals(err.code, "BACK_ALREADY_REQUESTED")
  }

  fixtures.defaultGame.test("Successfully requested back") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      ex.suc(_) -> _ <- appClient.backToMove(invite.id, invite.player1)(1)
    } yield ()
  }

  fixtures.defaultGame.test(
    "When trying to back to the move concurrently, only one will be successful") {
    invite =>
      for {
        _ <- appClient.move(invite.id, invite.player1)(A \ `1`)

        backToMove = appClient.backToMove(invite.id, invite.player1)(1)

        seqResults <- Seq(backToMove, backToMove, backToMove).parSequence
      } yield assertEquals(seqResults.count(_._1.isValid), 1)
  }
}
