package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Cell, Player, PlayerWithGame}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class GameOutput(player: Player,
                      opponent: Player,
                      ships: Seq[Cell], //todo: заменить на Set
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

  implicit val gameOutputEncoder: Encoder[GameOutput] = deriveEncoder
  implicit val gameOutputDecoder: Decoder[GameOutput] = deriveDecoder
}
