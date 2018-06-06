package io.othree.butler.client

import akka.actor.{ActorSystem, PoisonPill, Props}
import io.othree.butler.client.actors.CacheClientActor
import io.othree.butler.{Cache, CacheContainer}
import io.othree.butler.client.configuration.CacheClientConfiguration

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object CacheClient {
  def apply[A <: Cache](cacheClientConfiguration: CacheClientConfiguration)
           (implicit system: ActorSystem, ec: ExecutionContext, ct: ClassTag[A]): CacheClient[A] = {
    new CacheClient(system, cacheClientConfiguration)
  }
}

class CacheClient[A <: Cache] private(system: ActorSystem,
                              cacheClientConfiguration: CacheClientConfiguration)
                             (implicit ec: ExecutionContext, ct: ClassTag[A])
  extends CacheContainer[A] {

  private final val TOPIC = ct.runtimeClass.getSimpleName

  private final val cacheClientActor = system.actorOf(Props(new CacheClientActor[A](this, cacheClientConfiguration)), s"cache-client-$TOPIC")

  def terminate(): Unit = {
    cacheClientActor ! PoisonPill
  }
}
