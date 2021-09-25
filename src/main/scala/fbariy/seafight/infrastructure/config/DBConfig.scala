package fbariy.seafight.infrastructure.config

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

final case class MigrationConfig(table: String, locations: List[String])
final case class DBConfig(url: String,
                          driver: String,
                          user: String,
                          password: String,
                          migration: MigrationConfig)

object DBConfig {
  def transactor[F[_]: Async: ContextShift](
      dbc: DBConfig,
      ec: ExecutionContext,
      b: Blocker
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor
      .newHikariTransactor(dbc.driver, dbc.url, dbc.user, dbc.password, ec, b)
}
