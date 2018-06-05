package io.othree.butler.configuration

import scala.concurrent.duration.FiniteDuration

trait CacheServiceConfiguration {

  def refreshTime(topic: String): FiniteDuration

  def recoveryWaitTime: FiniteDuration
}
