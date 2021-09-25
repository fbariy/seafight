package util

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import munit.Suite
import org.testcontainers.containers.ContainerState

import java.io.File

trait AppEnvironmentForAllTests extends TestContainerForAll {
  this: Suite =>

  override val containerDef: DockerComposeContainer.Def =
    DockerComposeContainer.Def(
      new File("docker-compose.test.yml"),
      Seq(new ExposedService("db_1", 5432), new ExposedService("app_1", 8000))
    )

  lazy val db: ContainerState = withContainers(
    _.getContainerByServiceName("db_1").get)

  lazy val app: ContainerState = withContainers(
    _.getContainerByServiceName("app_1").get)
}
