package fbariy.seafight.infrastructure.repository

import cats.effect.Sync
import fbariy.seafight.application.back.BackToMoveRepository
import fbariy.seafight.domain.{Player, Turn}

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

class InMemoryBackToMoveRepository[F[_]: Sync] extends BackToMoveRepository[F] {
  private val requests =
    new ConcurrentHashMap[UUID, (Player, Turn)].asScala

  override def add(gameId: UUID, p: Player, turn: Turn): F[Unit] =
    Sync[F].delay(requests += gameId -> (p, turn))

  override def release(gameId: UUID): F[Option[(Player, Turn)]] =
    Sync[F].delay {
      for {
        maybeRes <- requests.get(gameId)
        _        <- requests.remove(gameId)
      } yield maybeRes
    }

  override def has(gameId: UUID): F[Boolean] =
    Sync[F].delay(requests.exists(a => a._1 == gameId))
}
