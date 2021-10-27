package fbariy.seafight.application.game

import fbariy.seafight.domain.{Cell, Player}
import fbariy.seafight.infrastructure.PlayerWithGame

case class GameOutput(player: Player,
                      opponent: Player,
                      ships: Seq[Cell],
                      playerTurns: Seq[TurnOutput],
                      opponentTurns: Seq[TurnOutput],
                      winner: Option[Player])
object GameOutput {
  def apply(played: PlayerWithGame): GameOutput =
    GameOutput(
      played.p,
      played.opp,
      if (played.isFirst) played.game.p1Ships else played.game.p2Ships,
      played.game.playerTurns(played.p).map(TurnOutput(_)),
      played.game.playerTurns(played.opp).map(TurnOutput(_)),
      played.game.winner
    )
}
