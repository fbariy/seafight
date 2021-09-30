package fbariy.seafight.infrastructure

import cats.Applicative
import cats.data.ValidatedNec
import cats.effect.{Bracket, Sync}
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.game.GameOutput
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.application.ship.AddShipsOutput
import fbariy.seafight.domain.{Cell, Player}
import org.http4s.Method.{GET, POST}
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Header,
  Headers,
  Request,
  Response,
  Uri
}
import org.http4s.client.Client
import fbariy.seafight.infrastructure.codec._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import cats.implicits._

import java.util.UUID

class SeafightClient[F[_]: Applicative: Sync: Bracket[*[_], Throwable]](
    httpClient: Client[F],
    baseUri: Uri) {

  def canMakeMove(
      gameId: UUID,
      p: Player): F[(ValidatedNec[AppErrorOutput, Boolean], Response[F])] =
    httpClient
      .run(
        Request[F](GET,
                   baseUri / "api" / "v1" / "game" / "can-make-move",
                   headers = Headers.of(Header("GameId", gameId.toString),
                                        Header("Player", p.name))))
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
          headers = Headers.of(Header("InviteId", inviteId.toString),
                               Header("Player", p.name))
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
          headers = Headers.of(Header("GameId", gameId.toString),
                               Header("Player", p.name))
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
