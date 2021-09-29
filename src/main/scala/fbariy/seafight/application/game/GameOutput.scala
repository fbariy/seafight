package fbariy.seafight.application.game

import fbariy.seafight.domain.{Cell, Player}
import fbariy.seafight.infrastructure.PlayerWithGame

case class GameOutput(player: Player,
                      opponent: Player,
                      ships: Seq[Cell],
                      playerTurns: Seq[TurnOutput],
                      opponentTurns: Seq[TurnOutput])
object GameOutput {
  def apply(played: PlayerWithGame): GameOutput =
    GameOutput(
      played.p,
      played.opp,
      if (played.isFirstPlayer) played.game.p1Ships else played.game.p2Ships,
      played.game.getPlayerTurns(played.p).map(TurnOutput(_)),
      played.game.getPlayerTurns(played.opp).map(TurnOutput(_))
    )
}
