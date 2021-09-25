package fbariy.seafight.infrastructure

import cats.Monad
import cats.data.Validated.Invalid
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import fbariy.seafight.application.errors._
import fbariy.seafight.application.game.GameRepo
import fbariy.seafight.application.invite.InviteRepo
import fbariy.seafight.domain.{GameWithPlayers, Invite, Player}
import fbariy.seafight.infrastructure.codec._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString

import java.util.UUID
import scala.util.Try

package object endpoint {
  def withGame[F[_]: Monad](gameRepo: GameRepo[F])(
      routes: AuthedRoutes[PlayerWithGame, F]): HttpRoutes[F] =
    new GameAuth[F](gameRepo).middleware(routes)

  def withInvite[F[_]: Monad](inviteRepo: InviteRepo[F])(
      routes: AuthedRoutes[PlayerWithInvite, F]
  ): HttpRoutes[F] =
    new InviteAuth[F](inviteRepo).middleware(routes)
}

case class PlayerWithInvite(p: Player, invite: Invite)
case class PlayerWithGame(p: Player, game: GameWithPlayers)

abstract class AbstractAuth[F[_]: Monad, V, R] {
  protected def validate(req: Request[F]): Either[AuthError, V]
  protected def getValue(validateRes: V): F[Either[AuthError, R]]

  private def auth: Kleisli[F, Request[F], Either[AuthError, R]] =
    Kleisli { req =>
      validate(req) match {
        case Left(e) => Either.left[AuthError, R](e).pure[F]
        case Right(res) =>
          getValue(res)
      }
    }

  private def onFailure: AuthedRoutes[AuthError, F] =
    Kleisli { authedReq =>
      OptionT.liftF[F, Response[F]](errorToResponse(authedReq.context))
    }

  private def errorToResponse(error: AuthError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    error match {
      case e @ MissingGameIdError => BadRequest(e.invalid[Unit].toValidatedNec)
      case e @ MissingPlayerError => BadRequest(e.invalid[Unit].toValidatedNec)
      case e @ MissingInviteIdError =>
        BadRequest(e.invalid[Unit].toValidatedNec)
      case e @ BadFormatGameIdError(_) =>
        BadRequest(e.invalid[Unit].toValidatedNec)
      case e @ BadFormatInviteIdError(_) =>
        BadRequest(e.invalid[Unit].toValidatedNec)
      case e @ NotFoundGameError   => NotFound(e.invalid[Unit].toValidatedNec)
      case e @ NotFoundInviteError => NotFound(e.invalid[Unit].toValidatedNec)
    }
  }

  protected def getHeaderValue[E](req: Request[F], h: String)(
      e: => E): Either[E, String] =
    req.headers
      .get(CaseInsensitiveString(h))
      .map(_.value)
      .toRight(e)

  lazy val middleware: AuthMiddleware[F, R] =
    AuthMiddleware(auth, onFailure)
}

private class GameAuth[F[_]: Monad](gameRepo: GameRepo[F])
    extends AbstractAuth[F, (UUID, Player), PlayerWithGame] {

  override protected def validate(
      req: Request[F]): Either[AuthError, (UUID, Player)] =
    for {
      rawId  <- getGameId(req)
      id     <- formatGameId(rawId)
      player <- getPlayer(req)
    } yield (id, player)

  override protected def getValue(
      validateRes: (UUID, Player)): F[Either[AuthError, PlayerWithGame]] =
    getGame(validateRes._1, validateRes._2).map {
      case Left(e)      => Either.left[AuthError, PlayerWithGame](e)
      case r @ Right(_) => r
    }

  private def getGame(
      id: UUID,
      p: Player): F[Either[NotFoundGameError.type, PlayerWithGame]] =
    gameRepo
      .findByIdAndPlayer(id, p)
      .map(_.map(PlayerWithGame(p, _)).toRight(NotFoundGameError))

  private def getGameId(
      req: Request[F]): Either[MissingGameIdError.type, String] =
    getHeaderValue(req, "GameId")(MissingGameIdError)

  private def formatGameId(gameId: String): Either[BadFormatGameIdError, UUID] =
    Try(UUID.fromString(gameId)).toEither.left
      .map(_ => BadFormatGameIdError(gameId))

  private def getPlayer(
      req: Request[F]): Either[MissingPlayerError.type, Player] =
    getHeaderValue(req, "Player")(MissingPlayerError).map(Player)
}

private class InviteAuth[F[_]: Monad](inviteRepo: InviteRepo[F])
    extends AbstractAuth[F, (UUID, Player), PlayerWithInvite] {
  override protected def validate(
      req: Request[F]): Either[AuthError, (UUID, Player)] =
    for {
      rawId    <- getInviteId(req)
      inviteId <- formatInviteId(rawId)
      player   <- getPlayer(req)
    } yield (inviteId, player)

  private def getInviteId(
      req: Request[F]): Either[MissingInviteIdError.type, String] =
    getHeaderValue(req, "InviteId")(MissingInviteIdError)

  private def formatInviteId(
      inviteId: String): Either[BadFormatInviteIdError, UUID] =
    Try(UUID.fromString(inviteId)).toEither.left
      .map(_ => BadFormatInviteIdError(inviteId))

  private def getPlayer(
      req: Request[F]): Either[MissingPlayerError.type, Player] =
    getHeaderValue(req, "Player")(MissingPlayerError).map(Player)

  override protected def getValue(
      validateRes: (UUID, Player)): F[Either[AuthError, PlayerWithInvite]] =
    validateRes match {
      case (id, p) =>
        inviteRepo
          .findByIdAndPlayer(id, p)
          .map(
            _.map(PlayerWithInvite(p, _))
              .toRight(NotFoundInviteError))
    }
}
