package fbariy.seafight.application.notification

import fbariy.seafight.infrastructure.PlayerContext

import java.util.UUID

trait NotificationBus[F[_]] {
  def enqueue(gameId: UUID,
              ctx: PlayerContext,
              notification: AppNotification): F[Unit]
  def dequeueAll(gameId: UUID,
                 ctx: PlayerContext): F[Option[List[AppNotification]]]
}
