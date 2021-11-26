package fbariy.seafight.core.infrastructure

import cats.data.ValidatedNec
import cats.effect.kernel.Concurrent
import cats.implicits._
import fbariy.seafight.core.application.notification.AppNotification
import fbariy.seafight.core.application.notification.instances._
import fbariy.seafight.core.application._
import fbariy.seafight.core.application.error.{AppError, ServerSystemError}
import fbariy.seafight.core.application.error.instances._
import fbariy.seafight.core.domain.{Cell, Player}
import fbariy.seafight.core.infrastructure.codec._
import org.http4s.Method.{GET, POST}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.typelevel.ci.CIStringSyntax

import java.util.UUID

class SeafightClient[F[_]: Concurrent](httpClient: Client[F],
                                       val baseUri: Uri) {

  def canMakeMove(
      gameId: UUID,
      p: Player): F[ValidatedNec[AppError, Boolean]] =
    httpClient
      .run(
        Request[F](GET,
                   baseUri / "api" / "v1" / "game" / "can-make-move",
                   headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                                     Header.Raw(ci"Player", p.name))))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, Boolean]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[Boolean]
            case Right(validated) => validated
          }
      }

  def createInvite(
      input: CreateInviteInput): F[ValidatedNec[AppError, InviteOutput]] =
    httpClient
      .run(
        Request[F](POST,
                   baseUri / "api" / "v1" / "preparation" / "invite",
                   body =
                     EntityEncoder[F, CreateInviteInput].toEntity(input).body))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, InviteOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[InviteOutput]
            case Right(validated) => validated
          }
      }

  //todo: заменить Seq => Set
  def addShips(inviteId: UUID, p: Player)(
      ships: Seq[Cell]): F[ValidatedNec[AppError, AddShipsOutput]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "preparation" / "ships",
          body = EntityEncoder[F, Seq[Cell]].toEntity(ships).body,
          headers = Headers(Header.Raw(ci"InviteId", inviteId.toString),
                            Header.Raw(ci"Player", p.name))
        ))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, AddShipsOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[AddShipsOutput]
            case Right(validated) => validated
          }
      }

  def move(gameId: UUID, p: Player)(
      kick: Cell): F[ValidatedNec[AppError, GameOutput]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "game" / "move",
          body = EntityEncoder[F, Cell].toEntity(kick).body,
          headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                            Header.Raw(ci"Player", p.name))
        )
      )
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[GameOutput]
            case Right(validated) => validated
          }
      }

  def getGame(gameId: UUID,
              p: Player): F[ValidatedNec[AppError, GameOutput]] = {
    httpClient
      .run(
        Request[F](
          GET,
          baseUri / "api" / "v1" / "game",
          headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                            Header.Raw(ci"Player", p.name))
        )
      )
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_) =>
              ServerSystemError.invalidNec[GameOutput]
            case Right(validated) => validated
          }
      }
  }

  def backToMove(gameId: UUID, p: Player)(
      moveNumber: Int): F[ValidatedNec[AppError, Unit]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "game" / "back" / moveNumber.toString,
          headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                            Header.Raw(ci"Player", p.name))
        )
      )
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, Unit]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[Unit]
            case Right(validated) => validated
          }
      }

  def acceptBack(gameId: UUID,
                 p: Player): F[ValidatedNec[AppError, GameOutput]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "game" / "accept-back",
          headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                            Header.Raw(ci"Player", p.name))
        )
      )
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[GameOutput]
            case Right(validated) => validated
          }
      }

  def cancelBack(gameId: UUID, p: Player): F[ValidatedNec[AppError, Unit]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "game" / "cancel-back",
          headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                            Header.Raw(ci"Player", p.name))
        )
      )
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, Unit]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[Unit]
            case Right(validated) => validated
          }
      }

  def release(inviteId: UUID,
              p: Player): F[ValidatedNec[AppError, List[AppNotification]]] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "notification" / "release",
          headers = Headers(Header.Raw(ci"InviteId", inviteId.toString),
                            Header.Raw(ci"Player", p.name))
        ))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, List[AppNotification]]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_) =>
              ServerSystemError.invalidNec[List[AppNotification]]
            case Right(validated) => validated
          }
      }

  def getInvite(inviteId: UUID,
                p: Player): F[ValidatedNec[AppError, InviteOutput]] =
    httpClient
      .run(
        Request[F](GET,
                   baseUri / "api" / "v1" / "preparation" / "invite",
                   headers =
                     Headers(Header.Raw(ci"InviteId", inviteId.toString),
                             Header.Raw(ci"Player", p.name))))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, InviteOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[InviteOutput]
            case Right(validated) => validated
          }
      }

  def canPlay(inviteId: UUID, p: Player): F[ValidatedNec[AppError, Unit]] =
    httpClient
      .run(
        Request[F](POST,
                   baseUri / "api" / "v1" / "preparation" / "can-play",
                   headers =
                     Headers(Header.Raw(ci"InviteId", inviteId.toString),
                             Header.Raw(ci"Player", p.name))))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppError, Unit]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(_)          => ServerSystemError.invalidNec[Unit]
            case Right(validated) => validated
          }
      }
}
