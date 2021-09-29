package util

import cats.effect.IO
import fbariy.seafight.infrastructure.config.{DBConfig, MigrationConfig}
import fbariy.seafight.infrastructure.migration.DBMigrations
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
    DBMigrations.clean[IO](dbConfig)

    //todo: этот код не вызывается
    super.afterEach(context)
  }
}
