package io.othree.butler.actors

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator._
import com.typesafe.scalalogging.LazyLogging
import io.othree.butler.configuration.CacheServiceConfiguration
import io.othree.butler.models.{EmptyCache, Gimme, RefreshCache}
import io.othree.butler.{Cache, CacheContainer, CacheProvider}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag

private[butler] class CacheActor[A <: Cache](cacheProvider: CacheProvider[A],
                                             cacheServiceConfiguration: CacheServiceConfiguration)
                                            (implicit ec: ExecutionContext, ct: ClassTag[A])
  extends Actor with LazyLogging {
  private val system = context.system

  private val cacheContainer = new CacheContainer[A]

  private final val TOPIC = ct.runtimeClass.getSimpleName

  private val mediator = DistributedPubSub(context.system).mediator
  logger.info(s"Adding self: ${self.path.toStringWithoutAddress} to mediator")
  mediator ! Put(self)

  system.scheduler.schedule(
    0 milliseconds,
    cacheServiceConfiguration.refreshTime(TOPIC),
    self,
    RefreshCache)

  override def receive: Receive = {
    case RefreshCache =>
      refreshCache()
    case Gimme =>
      val latestCache = cacheContainer.getCache()
      if (latestCache.isDefined) {
        logger.info(s"Sending cache to ${sender().path.toStringWithoutAddress}")
        sender() ! latestCache.get
      } else {
        logger.info(s"No cache set for topic: $TOPIC")
        sender() ! EmptyCache
      }
    case message: Any => logger.info(s"Message $message ignored")
  }

  private def refreshCache() = {
    logger.info(s"Refreshing cache. Topic: $TOPIC")
    val futureCache = cacheProvider.refreshCache()

    futureCache.foreach { updatedCache =>
      cacheContainer.setCache(updatedCache)
      logger.info(s"Publishing cache to topic: $TOPIC")
      mediator ! Publish(TOPIC, updatedCache)
    }

    futureCache.failed.foreach {
      case ex: Exception =>
        logger.error(s"Failed to update cache for topic: $TOPIC", ex)
        system.scheduler.scheduleOnce(cacheServiceConfiguration.recoveryWaitTime, self, RefreshCache)
    }
  }
}
