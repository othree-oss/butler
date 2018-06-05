package io.othree.butler

class CacheContainer[A <: Cache] {

  private var cache: Option[A] = None

  def getCache(): Option[A] = {
    cache
  }

  private[butler] def setCache(updatedCache: A) = {
    cache.synchronized {
      cache = Some(updatedCache)
    }
  }
}