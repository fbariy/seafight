import fbariy.seafight.core.domain.Digit._
import fbariy.seafight.core.domain.Symbol._
import util.AppSuite
import cats.implicits._
import fbariy.seafight.core.application.error._

class BackToMoveSuite extends AppSuite {
  fixtures.defaultGame.test("Specified move must be exist") { invite =>
    for {
      ex.errFirst(err) <- appClient.backToMove(invite.id, invite.player1)(1000)
    } yield assert(err.isInstanceOf[MoveIsNotExistError.type])
  }

  fixtures.defaultGame.test("Only one back can be requested") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      ex.suc(_)        <- appClient.backToMove(invite.id, invite.player1)(1)
      ex.errFirst(err) <- appClient.backToMove(invite.id, invite.player1)(1)
    } yield assert(err.isInstanceOf[BackAlreadyRequestedError.type])
  }

  fixtures.defaultGame.test("Successfully requested back") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      ex.suc(_) <- appClient.backToMove(invite.id, invite.player1)(1)
    } yield ()
  }

  fixtures.defaultGame.test(
    "When trying to back to the move concurrently, only one will be successful") {
    invite =>
      for {
        _ <- appClient.move(invite.id, invite.player1)(A \ `1`)

        backToMove = appClient.backToMove(invite.id, invite.player1)(1)

        seqResults <- Seq(backToMove, backToMove, backToMove).parSequence
      } yield assertEquals(seqResults.count(_.isValid), 1)
  }

  fixtures.defaultGame.test("Player can't cancel a not-existent back") {
    invite =>
      for {
        ex.errFirst(err) <- appClient.cancelBack(invite.id, invite.player1)
      } yield assert(err.isInstanceOf[BackNotRequestedError.type])
  }

  fixtures.defaultGame.test("Player can cancel an existing back") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      _ <- appClient.backToMove(invite.id, invite.player1)(1)

      ex.suc(_) <- appClient.cancelBack(invite.id, invite.player1)
    } yield ()
  }

  fixtures.defaultGame.test("Player can't accept a not-existent back") {
    invite =>
      for {
        ex.errFirst(err) <- appClient.acceptBack(invite.id, invite.player1)
      } yield assert(err.isInstanceOf[BackNotRequestedError.type])
  }

  fixtures.defaultGame.test("Only opponent can accept back") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      _ <- appClient.backToMove(invite.id, invite.player1)(1)

      ex.errFirst(err) <- appClient.acceptBack(invite.id, invite.player1)
    } yield assert(err.isInstanceOf[InitiatorCannotAcceptBackError.type])
  }

  fixtures.defaultGame.test("Opponent can accept back") { invite =>
    for {
      _ <- appClient.move(invite.id, invite.player1)(A \ `1`)
      _ <- appClient.move(invite.id, invite.player2)(A \ `2`)

      _ <- appClient.backToMove(invite.id, invite.player1)(1)

      ex.suc(game) <- appClient.acceptBack(invite.id, invite.player2)
    } yield
      assert(!(game.playerTurns ++ game.opponentTurns).exists(_.serial > 1))
  }
}
