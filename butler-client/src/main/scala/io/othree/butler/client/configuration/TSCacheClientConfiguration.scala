package io.othree.butler.client.configuration

import com.typesafe.config.Config
import io.othree.dna.TSConfigurationProvider

import scala.concurrent.duration.{Duration, FiniteDuration}

class TSCacheClientConfiguration(config: Config) extends TSConfigurationProvider(config) with CacheClientConfiguration {

  override def recoveryWaitTime: FiniteDuration = {
    val duration = Duration(getProperty("butler.cache.client.recovery-wait-time"))

    Some(duration).collect { case finiteDuration: FiniteDuration => finiteDuration }.get
  }
}
