package fbariy.seafight.core.domain

import java.util.UUID

case class GameWithPlayers(id: UUID,
                           p1Ships: Seq[Cell],
                           p2Ships: Seq[Cell],
                           turns: Seq[Turn],
                           p1: Player,
                           p2: Player,
                           winner: Option[Player] = None)

object GameWithPlayers {
  def playerTurns(moves: Seq[Turn], p: Player): Seq[Turn] =
    moves.filter(_.p == p)

  def checkWinner(moves: Seq[Turn],
                  p1: Player,
                  p1Ships: Seq[Cell],
                  p2: Player,
                  p2Ships: Seq[Cell]): Option[Player] =
    if (checkGameOver(p1Ships, playerTurns(moves, p2).map(_.kick)))
      Some(p2)
    else if (checkGameOver(p2Ships, playerTurns(moves, p1).map(_.kick)))
      Some(p1)
    else None

  def checkGameOver(ships: Seq[Cell], kicks: Seq[Cell]): Boolean =
    if (kicks.size < 20) false
    else (kicks intersect ships).size >= 20

  implicit class GameWithPlayersOps(game: GameWithPlayers) {
    def playerTurns(p: Player): Seq[Turn] =
      GameWithPlayers.playerTurns(game.turns, p)

    def nextSerial: Int =
      game.turns.maxByOption(_.serial).map(_.serial + 1).getOrElse(1)

    def addMove(p: Player, kick: Cell): GameWithPlayers =
      updateMoves(Turn(p, kick, nextSerial) +: game.turns)

    def updateMoves(moves: Seq[Turn]): GameWithPlayers =
      game.copy(
        turns = moves,
        winner =
          checkWinner(moves, game.p1, game.p1Ships, game.p2, game.p2Ships))
  }
}
