package fbariy.seafight.server.infrastructure.notification

import cats.effect.Sync
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.PlayerContext
import fbariy.seafight.server.application.notification.NotificationBus

import java.util.UUID
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Queue

//todo: захардкоженная реализация
final class InMemoryNotificationBus[F[_]: Sync] extends NotificationBus[F] {
  private val notificationQueues
    : TrieMap[UUID, (Queue[AppNotification], Queue[AppNotification])] =
    TrieMap.empty

  override def enqueue(gameId: UUID,
                       ctx: PlayerContext,
                       notification: AppNotification): F[Unit] =
    Sync[F].delay {
      notificationQueues.updateWith(gameId) {
        case Some(p1Queue -> p2Queue) =>
          Some(
            p1Queue.enqueue(notification) -> p2Queue.enqueue(notification)
          )
        case None =>
          Some(
            Queue(notification) -> Queue(notification)
          )
      }
      ()
    }

  override def dequeueAll(
      gameId: UUID,
      ctx: PlayerContext): F[Option[List[AppNotification]]] =
    Sync[F].delay {
      for {
        p1Queue -> p2Queue <- notificationQueues.get(gameId)
        notifications -> updatedQueues = if (ctx.isFirst)
          p1Queue.toList -> (Queue.empty, p2Queue)
        else
          p2Queue.toList -> (p1Queue, Queue.empty)
        _ = notificationQueues.update(gameId, updatedQueues)
      } yield notifications
    }
}
