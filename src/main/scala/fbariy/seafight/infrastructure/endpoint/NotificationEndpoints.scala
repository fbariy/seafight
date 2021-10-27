package fbariy.seafight.infrastructure.endpoint

import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.application.notification.ReleaseNotificationsHandler
import fbariy.seafight.infrastructure.PlayerWithInvite
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import fbariy.seafight.infrastructure.codec._
import org.http4s.circe.CirceEntityEncoder._

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
