package util

import cats.effect.IO
import fbariy.seafight.server.infrastructure.config.{DBConfig, MigrationConfig}
import fbariy.seafight.server.infrastructure.migration.DBMigrations
import munit.CatsEffectSuite

trait AppDBSchemaAware extends CatsEffectSuite {
  this: AppEnvironmentForAllTests =>

  private lazy val dbConfig: DBConfig =
    DBConfig(
      s"jdbc:postgresql://${db.getHost}:${db.getFirstMappedPort}/dbuser",
      "org.postgresql.Driver",
      "dbuser",
      "123456",
      MigrationConfig("migrations", List("classpath:migrations"))
    )

  override def beforeEach(context: BeforeEach): Unit = {
    DBMigrations.migrate[IO](dbConfig).unsafeRunSync()

    super.beforeEach(context)
  }

  override def afterEach(context: AfterEach): Unit = {
    DBMigrations.clean[IO](dbConfig).unsafeRunSync()

    super.afterEach(context)
  }
}
