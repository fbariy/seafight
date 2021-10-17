package fbariy.seafight.infrastructure.notification

import cats.effect.Sync
import fbariy.seafight.application.notification._
import fbariy.seafight.application.notification.NotificationBus
import java.util.concurrent.ConcurrentLinkedQueue

final class InMemoryNotificationBus[F[_]: Sync] extends NotificationBus[F] {
  private val queue: ConcurrentLinkedQueue[AppNotification] =
    new ConcurrentLinkedQueue[AppNotification]

  override def enqueue(notification: AppNotification): F[Boolean] =
    Sync[F].delay(queue.add(notification))

  override def dequeue: F[Option[AppNotification]] = Sync[F].delay {
    if (queue.isEmpty) None else Some(queue.poll())
  }

  override def dequeueAll: F[List[AppNotification]] = Sync[F].delay {
    LazyList.continually(queue.poll()).takeWhile(_ != null).toList
  }
}
