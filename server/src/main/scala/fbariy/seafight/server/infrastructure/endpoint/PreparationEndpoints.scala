package fbariy.seafight.server.infrastructure.endpoint

import cats.effect.kernel.Concurrent
import cats.implicits._
import fbariy.seafight.core.application.{CreateInviteInput, InviteOutput}
import fbariy.seafight.core.application.error.instances._
import fbariy.seafight.core.domain.{Cell, PlayerWithInvite}
import fbariy.seafight.core.infrastructure.codec._
import fbariy.seafight.server.application.canplay.CanPlayHandler
import fbariy.seafight.server.application.invite.CreateInviteHandler
import fbariy.seafight.server.application.ship.AddShipsHandler
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

class PreparationEndpoints[F[_]: Concurrent] extends Http4sDsl[F] {
  def createInvite(handler: CreateInviteHandler[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "invite" =>
        for {
          input     <- req.as[CreateInviteInput]
          validated <- handler.handle(input)
          ok        <- Ok(validated)
        } yield ok
    }

  def addShips(handler: AddShipsHandler[F]): AuthedRoutes[PlayerWithInvite, F] =
    AuthedRoutes.of[PlayerWithInvite, F] {
      case authReq @ POST -> Root / "ships" as inviteCtx =>
        for {
          ships     <- authReq.req.as[Seq[Cell]]
          validated <- handler.handle(ships, inviteCtx)
          ok        <- Ok(validated)
        } yield ok
    }

  def getInvite: AuthedRoutes[PlayerWithInvite, F] =
    AuthedRoutes.of[PlayerWithInvite, F] {
      case GET -> Root / "invite" as inviteCtx =>
        Ok(InviteOutput(inviteCtx.invite))
    }

  def canPlay(handler: CanPlayHandler[F]): AuthedRoutes[PlayerWithInvite, F] =
    AuthedRoutes.of[PlayerWithInvite, F] {
      case POST -> Root / "can-play" as inviteCtx =>
        for {
          validated <- handler.handle(inviteCtx)
          response <- Ok(validated)
        } yield response
    }
}
