import util.AppSuite

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class TestSuite extends AppSuite {
  test("test #1") {
    println(appClient.baseUri.toString())
    Thread.sleep(900000)
    val a = 10
  }

  override def munitTimeout: Duration = new FiniteDuration(3000, TimeUnit.SECONDS)
}
