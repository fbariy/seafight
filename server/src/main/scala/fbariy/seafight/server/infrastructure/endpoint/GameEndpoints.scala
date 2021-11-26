package fbariy.seafight.server.infrastructure.endpoint

import cats.effect.kernel.Concurrent
import fbariy.seafight.server.application.game.{CanMakeMoveHandler, MoveHandler}
import fbariy.seafight.core.domain.{Cell, PlayerWithGame}
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import fbariy.seafight.core.application.GameOutput
import fbariy.seafight.core.application.error.instances._
import fbariy.seafight.core.infrastructure.codec._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import cats.implicits._
import fbariy.seafight.server.application.back.{
  AcceptBackHandler,
  BackToMoveHandler,
  CancelBackHandler
}

class GameEndpoints[F[_]: Concurrent] extends Http4sDsl[F] {
  @deprecated("implements by notifications")
  def canMakeMove(
      handler: CanMakeMoveHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case GET -> Root / "can-make-move" as played =>
        for {
          canMakeMove <- handler.handle(played)
          response    <- Ok(canMakeMove)
        } yield response
    }

  def move(handler: MoveHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case authReq @ POST -> Root / "move" as played =>
        for {
          kick      <- authReq.req.as[Cell]
          validated <- handler.handle(played, kick)
          response  <- Ok(validated)
        } yield response
    }

  def getGame: AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case GET -> Root as played =>
        for {
          game     <- played
          response <- Ok(GameOutput(game))
        } yield response
    }

  def backToMove(
      handler: BackToMoveHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case POST -> Root / "back" / IntVar(moveNumber) as played =>
        for {
          validated <- handler.handle(played, moveNumber)
          response  <- Ok(validated)
        } yield response
    }

  def cancelBack(
      handler: CancelBackHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case POST -> Root / "cancel-back" as played =>
        for {
          validated <- handler.handle(played)
          response  <- Ok(validated)
        } yield response
    }

  def acceptBack(
      handler: AcceptBackHandler[F]): AuthedRoutes[F[PlayerWithGame], F] =
    AuthedRoutes.of[F[PlayerWithGame], F] {
      case POST -> Root / "accept-back" as played =>
        for {
          validated <- handler.handle(played)
          response  <- Ok(validated)
        } yield response
    }
}
