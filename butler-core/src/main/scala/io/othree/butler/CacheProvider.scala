package io.othree.butler

import scala.concurrent.Future

trait CacheProvider[A] {
  def refreshCache(): Future[A]
}
