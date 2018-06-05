package io.othree.butler.client.configuration

import scala.concurrent.duration.FiniteDuration

trait CacheClientConfiguration {
  def recoveryWaitTime: FiniteDuration
}
