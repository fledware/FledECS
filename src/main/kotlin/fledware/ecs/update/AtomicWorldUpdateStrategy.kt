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
 * This update strategy will put each world that needs
 * to be updated into a job and update the individual worlds
 * concurrently and only on a single thread.
 *
 * This allows threaded performance if there are many Worlds
 * that each hold data that needs heavy processing.
 */
class AtomicWorldUpdateStrategy(private val pool: BurstCyclicalJobPool = BurstCyclicalJobPool())
  : EngineUpdateStrategy {
  private val worlds = linkedMapOf<WorldManaged, () -> Unit>()
  private var worldsCache: Array<WorldManaged>? = null
  private val worldsArray
    get() = worldsCache ?: run {
      worldsCache = worlds.keys.toTypedArray()
      worldsCache!!
    }
  private var delta = 0f

  override fun worldBuilder(engine: Engine, name: String, options: Any?): WorldBuilder {
    return DefaultWorldBuilder(engine, name, options)
  }

  override fun entityGroup(include: (entity: Entity) -> Boolean): EntityGroupManaged {
    return DefaultEntityGroup(include)
  }

  override fun update(delta: Float) {
    this.delta = delta
    val worldsArray = worldsArray
    worldsArray.forEach { it.preUpdate() }
    pool.execute()
    worldsArray.forEach { it.postUpdate() }
  }

  override fun registerWorld(world: WorldManaged) {
    if (world in worlds)
      return
    val runnable = { world.update(delta) }
    worlds[world] = runnable
    pool.addJob(runnable)
    worldsCache = null
  }

  override fun unregisterWorld(world: WorldManaged) {
    val runnable = worlds.remove(world) ?: return
    pool.removeJob(runnable)
    worldsCache = null
  }

  override fun setThreadContext(context: ClassLoader) {
    pool.setContext(context)
  }

  override fun start(engine: Engine) {
    pool.start()
  }

  override fun shutdown() {
    pool.shutdown()
  }
}