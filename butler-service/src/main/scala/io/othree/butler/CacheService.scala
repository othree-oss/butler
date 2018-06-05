package io.othree.butler

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import io.othree.butler.configuration.CacheServiceConfiguration
import io.othree.butler.models.RefreshCache
import io.othree.butler.actors.CacheActor

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object CacheService {
  def apply[A <: Cache](cacheProvider: CacheProvider[A],
                        cacheServiceConfiguration: CacheServiceConfiguration)
                       (implicit system: ActorSystem, ec: ExecutionContext, ct: ClassTag[A]): CacheService = {
    val actorRef = system.actorOf(Props(new CacheActor[A](cacheProvider, cacheServiceConfiguration)), ct.runtimeClass.getSimpleName)
    new CacheService(actorRef)
  }
}

class CacheService(private val serviceActor: ActorRef) {
  def terminate(): Unit = {
    serviceActor ! PoisonPill
  }

  def refreshCache(): Unit = {
    serviceActor ! RefreshCache
  }
}
