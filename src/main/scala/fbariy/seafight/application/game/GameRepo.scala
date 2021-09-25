package fbariy.seafight.application.game

import fbariy.seafight.domain.{Game, GameWithPlayers, Player}

import java.util.UUID

trait GameRepo[F[_]] {
  def find(id: UUID): F[Option[GameWithPlayers]]
  def findByIdAndPlayer(id: UUID, p: Player): F[Option[GameWithPlayers]]
  def add(game: Game): F[Game]
}
