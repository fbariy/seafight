package fbariy.seafight.infrastructure.client

import cats.Applicative
import cats.data.ValidatedNec
import cats.effect.{Bracket, Sync}
import cats.implicits._
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.game.GameOutput
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.application.ship.AddShipsOutput
import fbariy.seafight.domain.{Cell, Player}
import fbariy.seafight.infrastructure.codec._
import org.http4s.Method.{GET, POST}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.headers.Accept
import org.typelevel.ci.CIStringSyntax
import org.http4s.client.dsl.Http4sClientDsl

import java.util.UUID

class SeafightClient[F[_]: Applicative: Sync: Bracket[*[_], Throwable]](
    httpClient: Client[F],
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
    val dsl = new Http4sClientDsl[F] {}
    import dsl._

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
}
