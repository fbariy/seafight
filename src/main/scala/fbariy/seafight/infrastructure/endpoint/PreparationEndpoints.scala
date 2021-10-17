package fbariy.seafight.infrastructure.endpoint

import cats.effect.{ContextShift, Sync}
import cats.implicits._
import fbariy.seafight.application.invite.{CreateInviteHandler, CreateInviteInput}
import fbariy.seafight.application.ship.AddShipsHandler
import fbariy.seafight.domain.Cell
import fbariy.seafight.infrastructure.PlayerWithInvite
import fbariy.seafight.infrastructure.codec._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes}

class PreparationEndpoints[F[_]: Sync: ContextShift] extends Http4sDsl[F] {
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
      case authReq @ POST -> Root / "ships" as invited =>
        for {
          ships     <- authReq.req.as[Seq[Cell]]
          validated <- handler.handle(ships, invited)
          ok        <- Ok(validated)
        } yield ok
    }
}
