package io.othree.butler

import io.othree.aok.BaseTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CacheContainerTest extends BaseTest {

  case class CacheTest() extends Cache

  "CacheContainer" when {
    "getting the cache" must {
      "return None if it hasn't been initialized" in {
        val container = new CacheContainer[CacheTest]

        val cache = container.getCache()
        cache shouldBe empty
      }
    }

    "setting the cache" must {
      "set the cache to the provided value" in {
        val container = new CacheContainer[CacheTest]
        container.setCache(CacheTest())

        val cache = container.getCache()
        cache shouldBe defined
        cache.get shouldEqual CacheTest()
      }
    }
  }
}
