package util

import cats.Applicative
import cats.data.ValidatedNec
import cats.effect.{Bracket, IO, Sync}
import cats.implicits._
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.application.ship.AddShipsOutput
import fbariy.seafight.domain.{Cell, Player}
import fbariy.seafight.infrastructure.codec._
import munit.CatsEffectSuite
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import java.util.UUID
import scala.concurrent.ExecutionContext.global

trait AppHttpClientAware extends CatsEffectSuite {
  this: AppEnvironmentForAllTests =>

  val httpClientFixture: Fixture[Client[IO]] = ResourceSuiteLocalFixture(
    "http_client",
    BlazeClientBuilder[IO](global).resource)

  override def munitFixtures: Seq[Fixture[_]] =
    super.munitFixtures ++ Seq(httpClientFixture)

  lazy val baseUri: Uri = Uri
    .fromString(s"http://0.0.0.0:${app.getFirstMappedPort}")
    .getOrElse(fail("Base uri must be valid"))

  lazy val httpClient: Client[IO] = httpClientFixture()

  lazy val appClient: SeafightClient[IO] =
    new SeafightClient[IO](httpClient, baseUri)
}

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
            case Right(validated) => validated
          }
          .map { _ -> response }
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
            case Right(validated) => validated
          }
          .map { _ -> response }
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
            case Right(validated) => validated
          }
          .map { _ -> response }
      }
}
