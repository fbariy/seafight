package util

import cats.effect.IO
import fbariy.seafight.infrastructure.SeafightClient
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

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
