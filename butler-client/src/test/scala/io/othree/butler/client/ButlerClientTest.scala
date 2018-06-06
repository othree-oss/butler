package io.othree.butler.client

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.othree.akkaok.BaseAkkaTest
import io.othree.butler.Cache
import io.othree.butler.client.configuration.CacheClientConfiguration
import io.othree.butler.models.{EmptyCache, Gimme}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ButlerClientTest extends BaseAkkaTest(ActorSystem("ActorCacheTest")) with ImplicitSender {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class CacheTest() extends Cache

  class WrappedTestProbe(testProbe: TestProbe) extends Actor {
    override def receive: Receive = {
      case msg: Any => testProbe.ref.forward(msg)
    }
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ButlerClient" when {

    "asking for the newest cache and no response is given from the mediator" must {
      "schedule another try" in {

        val testActor = TestProbe()
        val serviceMock = system.actorOf(Props(new WrappedTestProbe(testActor)), "cache-service-CacheTest")
        val mediator = DistributedPubSub(system).mediator
        mediator ! Put(serviceMock)

        val cacheClientConfiguration = mock[CacheClientConfiguration]
        when(cacheClientConfiguration.recoveryWaitTime).thenReturn(20 milliseconds)

        val client = CacheClient[CacheTest](cacheClientConfiguration)

        testActor.expectMsg(Gimme)

        //No response will be sent back, waiting for another Gimme in the retry
        testActor.expectMsg(Gimme)

        client.terminate()
        serviceMock ! PoisonPill
      }
    }

    "receiving a cached object" must {
      "verify that object got cached" in {

        val testActor = TestProbe()
        val serviceMock = system.actorOf(Props(new WrappedTestProbe(testActor)), "cache-service-CacheTest")
        val mediator = DistributedPubSub(system).mediator
        mediator ! Put(serviceMock)

        val cacheClientConfiguration = mock[CacheClientConfiguration]
        when(cacheClientConfiguration.recoveryWaitTime).thenReturn(20 milliseconds)

        val client = CacheClient[CacheTest](cacheClientConfiguration)

        testActor.expectMsg(Gimme)

        testActor.reply(CacheTest())

        expectNoMessage(100 milliseconds)
        client.getCache() shouldBe defined
        client.getCache().get shouldEqual  CacheTest()

        client.terminate()
        serviceMock ! PoisonPill
      }
    }

    "asking for cache and receiving an empty cached object" must {
      "schedule another try" in {
        val testActor = TestProbe()
        val serviceMock = system.actorOf(Props(new WrappedTestProbe(testActor)), "cache-service-CacheTest")
        val mediator = DistributedPubSub(system).mediator
        mediator ! Put(serviceMock)

        val cacheClientConfiguration = mock[CacheClientConfiguration]
        when(cacheClientConfiguration.recoveryWaitTime).thenReturn(100 milliseconds)

        val client = CacheClient[CacheTest](cacheClientConfiguration)

        testActor.expectMsg(Gimme)

        mediator ! Publish("test", None)

        testActor.expectMsg(Gimme)

        client.terminate()
        serviceMock ! PoisonPill

      }
    }

    "receiving an empty cached object" must {
      "schedule another try" in {
        val testActor = TestProbe()
        val serviceMock = system.actorOf(Props(new WrappedTestProbe(testActor)), "cache-service-CacheTest")
        val mediator = DistributedPubSub(system).mediator
        mediator ! Put(serviceMock)

        val cacheClientConfiguration = mock[CacheClientConfiguration]
        when(cacheClientConfiguration.recoveryWaitTime).thenReturn(20 milliseconds)

        val client = CacheClient[CacheTest](cacheClientConfiguration)

        testActor.expectMsg(Gimme)

        testActor.reply(EmptyCache)

        testActor.expectMsg(Gimme)

        client.terminate()
        serviceMock ! PoisonPill
      }
    }

    "confirming the topic subscription" must {
      "verify that a Gimme message got sent" in {

        val testActor = TestProbe()
        val serviceMock = system.actorOf(Props(new WrappedTestProbe(testActor)), "cache-service-CacheTest")
        val mediator = DistributedPubSub(system).mediator
        mediator ! Put(serviceMock)

        val cacheClientConfiguration = mock[CacheClientConfiguration]
        when(cacheClientConfiguration.recoveryWaitTime).thenReturn(20 milliseconds)

        val client = CacheClient[CacheTest](cacheClientConfiguration)

        testActor.expectMsg(Gimme)

        client.terminate()
        serviceMock ! PoisonPill
      }
    }
  }

}
