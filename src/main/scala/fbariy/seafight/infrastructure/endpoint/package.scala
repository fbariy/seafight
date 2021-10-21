package fbariy.seafight.infrastructure

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import fbariy.seafight.application.error._
import fbariy.seafight.application.game.GameRepository
import fbariy.seafight.application.invite.InviteRepository
import fbariy.seafight.domain.{Cell, GameWithPlayers, Invite, Player, Turn}
import fbariy.seafight.infrastructure.codec._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.typelevel.ci.CIString

import java.util.UUID
import scala.util.Try

package object endpoint {
  def withGame[F[_]: Monad](gameRepo: GameRepository[F])(
      routes: AuthedRoutes[F[PlayerWithGame], F]): HttpRoutes[F] =
    new GameAuth[F](gameRepo).middleware(routes)

  def withInvite[F[_]: Monad](inviteRepo: InviteRepository[F])(
      routes: AuthedRoutes[PlayerWithInvite, F]
  ): HttpRoutes[F] =
    new InviteAuth[F](inviteRepo).middleware(routes)
}

case class PlayerWithInvite(p: Player, invite: Invite)
case class PlayerWithGame(p: Player,
                          opp: Player,
                          isFirstPlayer: Boolean,
                          game: GameWithPlayers)
object PlayerWithGame {
  implicit class PlayerWithGameOps(playerCtx: PlayerWithGame) {
    def updateMoves(moves: Seq[Turn]): PlayerWithGame =
      playerCtx.copy(game = playerCtx.game.updateMoves(moves))

    def addMove(kick: Cell): PlayerWithGame =
      playerCtx.copy(game = playerCtx.game.addMove(playerCtx.p, kick))
  }
}

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
      .get(CIString(h))
      .map(_.head.value)
      .toRight(e)

  lazy val middleware: AuthMiddleware[F, R] =
    AuthMiddleware(auth, onFailure)
}

private class GameAuth[F[_]: Monad](gameRepo: GameRepository[F])
    extends AbstractAuth[F, (UUID, Player), F[PlayerWithGame]] {

  override protected def validate(
      req: Request[F]): Either[AuthError, (UUID, Player)] =
    for {
      rawId  <- getGameId(req)
      id     <- formatGameId(rawId)
      player <- getPlayer(req)
    } yield (id, player)

  override protected def getValue(
      validateRes: (UUID, Player)): F[Either[AuthError, F[PlayerWithGame]]] = {
    val gameId -> p = validateRes
    val findGame    = gameRepo.findByIdAndPlayer(gameId, p)

    findGame.map(
      maybeGame =>
        if (maybeGame.isEmpty) Either.left(NotFoundGameError)
        //игры не удаляются из приложения, поэтому допускаем её существование
        //во время выполнения F[PlayerWithGame]
        else Either.right(findGame.map(_.get).map(gameToPlayerContext(_, p))))
  }

  private def gameToPlayerContext(game: GameWithPlayers,
                                  p: Player): PlayerWithGame = {
    val isFirstPlayer = game.p1 == p

    PlayerWithGame(p,
                   if (isFirstPlayer) game.p2 else game.p1,
                   isFirstPlayer,
                   game)
  }

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

private class InviteAuth[F[_]: Monad](inviteRepo: InviteRepository[F])
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
