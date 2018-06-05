package io.othree.butler.client.actors

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Send, Subscribe, SubscribeAck}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.othree.butler.client.configuration.CacheClientConfiguration
import io.othree.butler.models.{EmptyCache, Gimme}
import io.othree.butler.{Cache, CacheContainer}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag

private[butler] class CacheClientActor[A <: Cache](cacheContainer: CacheContainer[A],
                                                   cacheClientConfiguration: CacheClientConfiguration)
                                                  (implicit ec: ExecutionContext, ct: ClassTag[A])
  extends Actor with LazyLogging {
  private val system = context.system

  private final val TOPIC = ct.runtimeClass.getSimpleName

  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(TOPIC, self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe(`TOPIC`, None, `self`)) =>
      askForCache(TOPIC)
    case Gimme =>
      askForCache(TOPIC)
    case cache: A =>
      setCache(cache)
  }

  private def askForCache(cacheTopic: String): Unit = {
    logger.info(s"Asking for latest cache to topic: $cacheTopic")
    implicit val timeout = Timeout(1 second)
    val future = mediator ? Send(s"/user/cache-service-$TOPIC", Gimme, localAffinity = true)
    future.foreach {
      case cache: A =>
        setCache(cache)
      case EmptyCache =>
        logger.info(s"No cache provided, waiting for ${cacheClientConfiguration.recoveryWaitTime.toSeconds} seconds to try again")
        system.scheduler.scheduleOnce(cacheClientConfiguration.recoveryWaitTime, self, Gimme)
    }
    future.failed.foreach {
      case exception: Exception =>
        logger.error(s"Failed to get response from mediator, waiting for ${cacheClientConfiguration.recoveryWaitTime.toSeconds} seconds to try again", exception)
        system.scheduler.scheduleOnce(cacheClientConfiguration.recoveryWaitTime, self, Gimme)
    }
  }

  private def setCache(cache: A): Unit = {
    logger.info(s"Updating cache from topic: $TOPIC")
    cacheContainer.setCache(cache)
  }
}