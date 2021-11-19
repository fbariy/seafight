package fbariy.seafight.server.application.notification

import cats.Functor
import cats.implicits._
import fbariy.seafight.core.application.notification._
import fbariy.seafight.core.domain.PlayerWithInvite

class ReleaseNotificationsHandler[F[_]: Functor](bus: NotificationBus[F]) {
  def handle(inviteCtx: PlayerWithInvite): F[List[AppNotification]] =
    bus
      .dequeueAll(inviteCtx.invite.id, inviteCtx)
      .map(_.getOrElse(List.empty))
}
