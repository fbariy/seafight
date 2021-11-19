package fbariy.seafight.server.infrastructure.endpoint

import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.core.application.notification.instances._
import fbariy.seafight.core.domain.PlayerWithInvite
import fbariy.seafight.server.application.notification.ReleaseNotificationsHandler
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

class NotificationEndpoints[F[_]: Sync] extends Http4sDsl[F] {
  def release(handler: ReleaseNotificationsHandler[F])
    : AuthedRoutes[PlayerWithInvite, F] =
    AuthedRoutes.of {
      case POST -> Root / "release" as inviteCtx =>
        for {
          notifications <- handler.handle(inviteCtx)
          result        <- Ok(notifications)
        } yield result
    }
}
