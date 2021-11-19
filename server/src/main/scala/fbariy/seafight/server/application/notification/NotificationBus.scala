package fbariy.seafight.server.application.notification

import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.PlayerContext

import java.util.UUID

trait NotificationBus[F[_]] {
  def enqueue(gameId: UUID,
              ctx: PlayerContext,
              notification: AppNotification): F[Unit]
  def dequeueAll(gameId: UUID,
                 ctx: PlayerContext): F[Option[List[AppNotification]]]
}
