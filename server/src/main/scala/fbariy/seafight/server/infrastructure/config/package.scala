package fbariy.seafight.server.infrastructure

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

package object config {
  implicit val migrationConfigReader: ConfigReader[MigrationConfig] = deriveReader
  implicit val databaseConfigReader: ConfigReader[DBConfig] = deriveReader
  implicit val appConfigReader: ConfigReader[AppConfig] = deriveReader
}
