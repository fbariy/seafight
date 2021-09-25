package fbariy.seafight.infrastructure.migration

import cats.effect.Sync
import com.typesafe.scalalogging.LazyLogging
import fbariy.seafight.infrastructure.config.DBConfig
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.Location
import org.flywaydb.core.Flyway

import scala.jdk.CollectionConverters._

object DBMigrations extends LazyLogging {
  def migrate[F[_]: Sync](config: DBConfig): F[Int] =
    Sync[F].delay {
      logger.info(
        "Running migrations from locations: " +
          config.migration.locations.mkString(", ")
      )
      val count = unsafeMigrate(config)
      logger.info(s"Executed $count migrations")
      count
    }

  def clean[F[_]: Sync](config: DBConfig): F[Unit] =
    Sync[F].delay {
      logger.info("Running clean...")
      prepareConf(config).load().clean()
      logger.info(s"Clean was execute")
    }

  private def unsafeMigrate(config: DBConfig): Int =
    prepareConf(config).load().migrate().migrationsExecuted

  private def prepareConf(config: DBConfig): FluentConfiguration = {
    val m: FluentConfiguration = Flyway.configure
      .dataSource(
        config.url,
        config.user,
        config.password
      )
      .group(true)
      .outOfOrder(false)
      .table(config.migration.table)
      .locations(
        config.migration.locations
          .map(new Location(_)): _*
      )
      .baselineOnMigrate(true)

    logValidationErrorsIfAny(m)
    m
  }

  private def logValidationErrorsIfAny(m: FluentConfiguration): Unit = {
    val validated = m
      .ignorePendingMigrations(true)
      .load()
      .validateWithResult()

    if (!validated.validationSuccessful)
      for (error <- validated.invalidMigrations.asScala)
        logger.warn(s"""
             |Failed validation:
             |  - version: ${error.version}
             |  - path: ${error.filepath}
             |  - description: ${error.description}
             |  - errorCode: ${error.errorDetails.errorCode}
             |  - errorMessage: ${error.errorDetails.errorMessage}
        """.stripMargin)
  }
}
