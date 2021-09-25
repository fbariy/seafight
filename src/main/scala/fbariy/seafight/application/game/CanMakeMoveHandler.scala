package fbariy.seafight.application.game

import fbariy.seafight.infrastructure.PlayerWithGame

class CanMakeMoveHandler {
  def handle(played: PlayerWithGame): Boolean = {
    import played._

    game.turns.headOption match {
      case Some(turn) => turn.p != p
      case None => game.p1 == p
    }
  }
}
