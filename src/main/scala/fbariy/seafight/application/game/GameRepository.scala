package fbariy.seafight.application.game

import fbariy.seafight.domain.{Game, GameWithPlayers, Player, Turn}

import java.util.UUID

trait GameRepository[F[_]] {
  def find(id: UUID): F[Option[GameWithPlayers]]
  def findByIdAndPlayer(id: UUID, p: Player): F[Option[GameWithPlayers]]
  def add(game: Game): F[Game]
  def updateTurns(id: UUID, turns: Seq[Turn]): F[Game]
  def updateGame(id: UUID, turns: Option[Seq[Turn]], winner: Option[Player]): F[GameWithPlayers]
}
