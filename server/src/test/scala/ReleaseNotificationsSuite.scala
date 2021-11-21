import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.Digit.`1`
import fbariy.seafight.core.domain.Symbol.A
import util.AppSuite

class ReleaseNotificationsSuite extends AppSuite {
  fixtures.defaultGame.test(
    "After the release, all player notifications are removed") { invite =>
    for {
      _                  <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.suc(ns)         <- appClient.release(invite.id, invite.player1)
      ex.suc(repeatedNs) <- appClient.release(invite.id, invite.player1)
    } yield {
      assert(clue(ns).nonEmpty)
      assert(clue(repeatedNs).isEmpty)
    }
  }

  fixtures.defaultGame.test(
    "Notifications available to both players of the game") { invite =>
    for {
      _            <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ex.suc(p1Ns) <- appClient.release(invite.id, invite.player1)
      ex.suc(p2Ns) <- appClient.release(invite.id, invite.player2)
    } yield {
      assert(clue(p1Ns).exists(_.isInstanceOf[MoveMadeNotification]))
      assert(clue(p2Ns).exists(_.isInstanceOf[MoveMadeNotification]))
    }
  }
}
