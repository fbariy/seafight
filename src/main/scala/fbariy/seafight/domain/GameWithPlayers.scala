package fbariy.seafight.domain

import java.util.UUID

case class GameWithPlayers(id: UUID,
                           p1Ships: Seq[Cell],
                           p2Ships: Seq[Cell],
                           turns: Seq[Turn],
                           p1: Player,
                           p2: Player) {
  def getPlayerTurns(p: Player): Seq[Turn] = turns.filter(_.p == p)
}
