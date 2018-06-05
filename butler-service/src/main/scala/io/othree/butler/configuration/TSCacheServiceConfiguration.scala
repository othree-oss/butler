package io.othree.butler.configuration

import com.typesafe.config.Config
import io.othree.dna.TSConfigurationProvider

import scala.concurrent.duration.{Duration, FiniteDuration}

class TSCacheServiceConfiguration(config: Config)
  extends TSConfigurationProvider(config)
  with CacheServiceConfiguration {

  override def refreshTime(topic: String): FiniteDuration = {
    val duration = Duration(getProperty(s"butler.cache.service.$topic.refresh-time"))

    Some(duration).collect { case finiteDuration: FiniteDuration => finiteDuration }.get
  }

  override def recoveryWaitTime: FiniteDuration = {
    val duration = Duration(getProperty("butler.cache.service.recovery-wait-time"))

    Some(duration).collect { case finiteDuration: FiniteDuration => finiteDuration }.get
  }
}
