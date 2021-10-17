package fbariy.seafight.application.notification

trait NotificationBus[F[_]] {
  def enqueue(notification: AppNotification): F[Boolean]
  def dequeue: F[Option[AppNotification]]
  def dequeueAll: F[List[AppNotification]]
}
