package io.othree.butler.client.configuration

import com.typesafe.config.ConfigFactory
import io.othree.aok.BaseTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TSCacheClientConfigurationTest extends BaseTest {
  var config: TSCacheClientConfiguration = _

  override def beforeAll(): Unit = {
    config = new TSCacheClientConfiguration(ConfigFactory.load())
  }

  "TSCacheClientConfiguration" when {
    "getting the recovery wait time" must {
      "returned the configured value" in {
        config.recoveryWaitTime shouldBe 20.seconds
      }
    }
  }
}