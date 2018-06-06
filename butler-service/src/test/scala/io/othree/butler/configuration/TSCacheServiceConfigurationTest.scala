package io.othree.butler.configuration

import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import io.othree.aok.BaseTest
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TSCacheServiceConfigurationTest extends BaseTest {
  var config: TSCacheServiceConfiguration = _

  override def beforeAll(): Unit = {
    config = new TSCacheServiceConfiguration(ConfigFactory.load())
  }

  "TSCacheServiceConfiguration" when {
    "getting the refresh time" must {
      "returned the configured value" in {
        config.refreshTime("topic") shouldBe 30.seconds
      }
    }
    "getting the recovery wait time" must {
      "returned the configured value" in {
        config.recoveryWaitTime shouldBe 10.seconds
      }
    }
  }
}
