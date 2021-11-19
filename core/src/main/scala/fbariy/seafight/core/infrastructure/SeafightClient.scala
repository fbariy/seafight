package fbariy.seafight.core.infrastructure

import cats.data.ValidatedNec
import cats.effect.kernel.Concurrent
import cats.implicits._
import fbariy.seafight.core.application.notification.AppNotification
import fbariy.seafight.core.application.notification.instances._
import fbariy.seafight.core.application._
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
      p: Player): F[(ValidatedNec[AppErrorOutput, Boolean], Response[F])] =
    httpClient
      .run(
        Request[F](GET,
                   baseUri / "api" / "v1" / "game" / "can-make-move",
                   headers = Headers(Header.Raw(ci"GameId", gameId.toString),
                                     Header.Raw(ci"Player", p.name))))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppErrorOutput, Boolean]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def createInvite(input: CreateInviteInput)
    : F[(ValidatedNec[AppErrorOutput, InviteOutput], Response[F])] =
    httpClient
      .run(
        Request[F](POST,
                   baseUri / "api" / "v1" / "preparation" / "invite",
                   body =
                     EntityEncoder[F, CreateInviteInput].toEntity(input).body))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppErrorOutput, InviteOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def createInviteBase(input: CreateInviteInput)
    : F[Either[(DecodeFailure, Response[F]),
               (ValidatedNec[AppErrorOutput, InviteOutput], Response[F])]] =
    httpClient
      .run(
        Request[F](POST,
                   baseUri / "api" / "v1" / "preparation" / "invite",
                   body =
                     EntityEncoder[F, CreateInviteInput].toEntity(input).body))
      .use { response =>
        EntityDecoder[F, ValidatedNec[AppErrorOutput, InviteOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => Either.left(failure    -> response)
            case Right(validated) => Either.right(validated -> response)
          }
      }

  def createInviteWithDecodeFailureInErrors(
      input: CreateInviteInput): F[ValidatedNec[AppErrorOutput, InviteOutput]] =
    createInviteBase(input).map {
      case Left(failure -> _) =>
        AppErrorOutput("DECODE_FAILURE", failure.message)
          .invalidNec[InviteOutput]
      case Right(validated -> _) => validated
    }

  def addShips(inviteId: UUID, p: Player)(ships: Seq[Cell])
    : F[(ValidatedNec[AppErrorOutput, AddShipsOutput], Response[F])] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, AddShipsOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def addShipsBase(inviteId: UUID, p: Player)(ships: Seq[Cell])
    : F[Either[(DecodeFailure, Response[F]),
               (ValidatedNec[AppErrorOutput, AddShipsOutput], Response[F])]] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, AddShipsOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => Either.left(failure    -> response)
            case Right(validated) => Either.right(validated -> response)
          }
      }

  //todo: заменить Seq => Set
  def addShipsWithDecodeFailureInErrors(inviteId: UUID, p: Player)(
      ships: Seq[Cell]): F[ValidatedNec[AppErrorOutput, AddShipsOutput]] =
    addShipsBase(inviteId, p)(ships).map {
      case Left(failure -> _) =>
        decodeFailure(failure.message).invalidNec[AddShipsOutput]
      case Right(validated -> _) => validated
    }

  private def decodeFailure(msg: String): AppErrorOutput =
    AppErrorOutput("DECODE_FAILURE", msg)

  def move(gameId: UUID, p: Player)(
      kick: Cell): F[(ValidatedNec[AppErrorOutput, GameOutput], Response[F])] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def getGame(
      gameId: UUID,
      p: Player): F[(ValidatedNec[AppErrorOutput, GameOutput], Response[F])] = {
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }
  }

  def backToMove(gameId: UUID, p: Player)(
      moveNumber: Int): F[(ValidatedNec[AppErrorOutput, Unit], Response[F])] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, Unit]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def acceptBack(
      gameId: UUID,
      p: Player): F[(ValidatedNec[AppErrorOutput, GameOutput], Response[F])] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, GameOutput]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def cancelBack(
      gameId: UUID,
      p: Player): F[(ValidatedNec[AppErrorOutput, Unit], Response[F])] =
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
        EntityDecoder[F, ValidatedNec[AppErrorOutput, Unit]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }

  def release(inviteId: UUID,
              p: Player): F[(List[AppNotification], Response[F])] =
    httpClient
      .run(
        Request[F](
          POST,
          baseUri / "api" / "v1" / "notification" / "release",
          headers = Headers(Header.Raw(ci"InviteId", inviteId.toString),
                            Header.Raw(ci"Player", p.name))
        ))
      .use { response =>
        EntityDecoder[F, List[AppNotification]]
          .decode(response, strict = true)
          .value
          .map {
            case Left(failure)    => throw failure
            case Right(validated) => validated -> response
          }
      }
}
