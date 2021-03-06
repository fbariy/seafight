package fbariy.seafight.server.application.back

import fbariy.seafight.core.domain.{Player, Turn}

import java.util.UUID

trait BackToMoveRepository[F[_]] {
  def add(gameId: UUID, p: Player, turn: Turn): F[Unit]
  def release(gameId: UUID): F[Option[(Player, Turn)]]
  def find(gameId: UUID): F[Option[(Player, Turn)]]
}
