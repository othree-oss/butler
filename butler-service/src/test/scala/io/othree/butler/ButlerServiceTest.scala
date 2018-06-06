package io.othree.butler.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Send, Subscribe, SubscribeAck}
import akka.testkit.{ImplicitSender, TestKit}
import io.othree.akkaok.BaseAkkaTest
import io.othree.butler.configuration.CacheServiceConfiguration
import io.othree.butler.models.{EmptyCache, Gimme}
import io.othree.butler.{ButlerService, Cache, CacheProvider}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

import scala.concurrent.Future
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ButlerServiceTest extends BaseAkkaTest(ActorSystem("ActorCacheTest")) with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class CacheTest() extends Cache

  var mediator : ActorRef = _
  var cacheServiceConfiguration : CacheServiceConfiguration = _

  override def beforeAll(): Unit = {
    mediator = DistributedPubSub(system).mediator
    mediator ! Subscribe("CacheTest", self)
    expectMsgClass(classOf[SubscribeAck])

    cacheServiceConfiguration = mock[CacheServiceConfiguration]
    when(cacheServiceConfiguration.refreshTime("CacheTest")).thenReturn(5 seconds)
    when(cacheServiceConfiguration.recoveryWaitTime).thenReturn(5 seconds)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "ButlerService" when {
    "receiving a RefreshCache message" must {
      "verify that refreshCache was invoked" in {
        val cacheProvider = mock[CacheProvider[CacheTest]]
        when(cacheProvider.refreshCache()).thenReturn(Future {
          CacheTest()
        })

        val butlerService = ButlerService(cacheProvider, cacheServiceConfiguration)

        expectMsg(CacheTest())

        butlerService.refreshCache()

        expectMsg(CacheTest())
        verify(cacheProvider, times(2)).refreshCache()

        butlerService.terminate()
      }
    }

    "the cache provider crashes" must {
      "not publish the current cache" in {
        val cacheProvider = mock[CacheProvider[CacheTest]]
        when(cacheProvider.refreshCache()).thenReturn(Future {
          throw new Exception("kapow!")
        })

        val butlerService = ButlerService(cacheProvider, cacheServiceConfiguration)

        expectNoMessage()

        butlerService.terminate()
      }
    }

    "receiving a Gimme" must {
      "reply the cache" in {
        val cacheProvider = mock[CacheProvider[CacheTest]]
        when(cacheProvider.refreshCache()).thenReturn(Future {
          CacheTest()
        })

        val butlerService = ButlerService(cacheProvider, cacheServiceConfiguration)

        expectMsg(CacheTest())

        mediator ! Send("/user/CacheTest", Gimme, localAffinity = true)

        val message = receiveOne(100 milliseconds)

        message shouldBe CacheTest()

        verify(cacheProvider, times(1)).refreshCache()

        butlerService.terminate()
      }
    }

    "receiving a Gimme and the cache is not set" must {
      "reply with an EmptyCache" in {
        val cacheProvider = mock[CacheProvider[CacheTest]]
        when(cacheProvider.refreshCache()).thenReturn(Future {
          Thread.sleep(1000)
          throw new Exception("kapow!")
        })

        val cacheService = ButlerService(cacheProvider, cacheServiceConfiguration)

        expectNoMessage()

        mediator ! Send("/user/CacheTest", Gimme, localAffinity = true)

        val message = receiveOne(100 milliseconds)

        message shouldBe EmptyCache

        verify(cacheProvider, times(1)).refreshCache()

        cacheService.terminate()
      }
    }
  }
}
