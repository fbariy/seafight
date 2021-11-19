package fbariy.seafight.server.application.game

import fbariy.seafight.core.domain.{Game, GameWithPlayers, Player, Turn}

import java.util.UUID

trait GameRepository[F[_]] {
  def find(id: UUID): F[Option[GameWithPlayers]]
  def findByIdAndPlayer(id: UUID, p: Player): F[Option[GameWithPlayers]]
  def add(game: Game): F[Game]
  def updateGame(game: GameWithPlayers): F[Unit]
}
