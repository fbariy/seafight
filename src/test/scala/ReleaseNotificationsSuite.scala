import fbariy.seafight.application.notification.MoveMadeNotification
import fbariy.seafight.domain.Digit.`1`
import fbariy.seafight.domain.Symbol.A
import util.AppSuite

class ReleaseNotificationsSuite extends AppSuite {
  fixtures.defaultGame.test(
    "After the release, all player notifications are removed") { invite =>
    for {
      _               <- appClient.move(invite.id, invite.player1)(A \ `1`)
      ns -> _         <- appClient.release(invite.id, invite.player1)
      repeatedNs -> _ <- appClient.release(invite.id, invite.player1)
    } yield {
      assert(clue(ns).nonEmpty)
      assert(clue(repeatedNs).isEmpty)
    }
  }

  fixtures.defaultGame.test(
    "Notifications available to both players of the game") { invite =>
    for {
      _         <- appClient.move(invite.id, invite.player1)(A \ `1`)
      p1Ns -> _ <- appClient.release(invite.id, invite.player1)
      p2Ns -> _ <- appClient.release(invite.id, invite.player2)
    } yield {
      assert(clue(p1Ns).exists(_.isInstanceOf[MoveMadeNotification]))
      assert(clue(p2Ns).exists(_.isInstanceOf[MoveMadeNotification]))
    }
  }
}
