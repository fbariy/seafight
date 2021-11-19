package fbariy.seafight
package client
package infrastructure.configuration

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class AppConfig(host: String)
object AppConfig {
  implicit val appConfigConfigReader: ConfigReader[AppConfig] = deriveReader
}
