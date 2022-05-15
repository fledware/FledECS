package fledware.ecs.update

import fledware.ecs.Engine
import fledware.ecs.EngineUpdateStrategy
import fledware.ecs.Entity
import fledware.ecs.EntityGroupManaged
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldManaged
import fledware.ecs.impl.DefaultEntityGroup
import fledware.ecs.impl.DefaultWorldBuilder

/**
 * Simple UpdateStrategy that just loops through
 * each world and updates them one at a time.
 */
class MainThreadUpdateStrategy : EngineUpdateStrategy {
  private val worlds = linkedSetOf<WorldManaged>()
  private var worldsCache: Array<WorldManaged>? = null
  private var updateContext = Thread.currentThread().contextClassLoader

  override fun worldBuilder(engine: Engine, name: String, options: Any?): WorldBuilder {
    return DefaultWorldBuilder(engine, name, options)
  }

  override fun entityGroup(include: (entity: Entity) -> Boolean): EntityGroupManaged {
    return DefaultEntityGroup(include)
  }

  override fun update(delta: Float) {
    if (worldsCache == null)
      worldsCache = worlds.toTypedArray()
    val worldsCache = worldsCache!!
    val context = Thread.currentThread().contextClassLoader
    try {
      Thread.currentThread().contextClassLoader = updateContext
      worldsCache.forEach { it.preUpdate() }
      worldsCache.forEach { it.update(delta) }
      worldsCache.forEach { it.postUpdate() }
    }
    finally {
      Thread.currentThread().contextClassLoader = context
    }
  }

  override fun registerWorld(world: WorldManaged) {
    worlds += world
    worldsCache = null
  }

  override fun unregisterWorld(world: WorldManaged) {
    worlds -= worlds
    worldsCache = null
  }

  override fun setThreadContext(context: ClassLoader) {
    updateContext = context
  }

  override fun start(engine: Engine) {

  }

  override fun shutdown() {

  }
}