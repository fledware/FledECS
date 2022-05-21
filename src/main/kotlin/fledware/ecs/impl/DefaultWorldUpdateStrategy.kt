package fledware.ecs.impl

import fledware.ecs.Engine
import fledware.ecs.EngineUpdateStrategy
import fledware.ecs.Entity
import fledware.ecs.EntityGroupManaged
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldManaged
import fledware.ecs.update.BurstJob
import fledware.ecs.update.BurstJobExecutorPool
import fledware.ecs.update.BurstJobMainThreadPool
import fledware.ecs.update.BurstJobPool
import fledware.ecs.util.CachedArray
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


fun mainThreadUpdateStrategy() =
    DefaultWorldUpdateStrategy(BurstJobMainThreadPool())

fun executorUpdateStrategy(threads: Int = Runtime.getRuntime().availableProcessors()) =
    DefaultWorldUpdateStrategy(BurstJobExecutorPool(Executors.newFixedThreadPool(threads), true))

fun executorUpdateStrategy(executor: ExecutorService) =
    DefaultWorldUpdateStrategy(BurstJobExecutorPool(executor, false))


/**
 * This update strategy will put each world that needs
 * to be updated into a job and update the individual worlds
 * concurrently and only on a single thread.
 *
 * This allows threaded performance if there are many Worlds
 * that each hold data that needs heavy processing.
 */
class DefaultWorldUpdateStrategy(private val burstJobPool: BurstJobPool)
  : EngineUpdateStrategy {
  private data class UpdateGroup(val name: String, val order: Int) {
    val jobs = arrayListOf<BurstJob>()
  }

  private val worldsToJob = linkedMapOf<WorldManaged, BurstJob>()
  private val worldsOrdered = CachedArray { worldsToJob.keys.toTypedArray() }

  private val groups = linkedMapOf<String, UpdateGroup>()
  private val groupsOrdered = CachedArray { groups.values.sortedBy { it.order }.toTypedArray() }

  private var delta = 0f

  override fun worldBuilder(engine: Engine, name: String, options: Any?): WorldBuilder {
    return DefaultWorldBuilder(engine, name, options)
  }

  override fun entityGroup(include: (entity: Entity) -> Boolean): EntityGroupManaged {
    return DefaultEntityGroup(include)
  }

  override fun createWorldUpdateGroup(name: String, order: Int) {
    groupsOrdered.clear()
    groups[name] = UpdateGroup(name, order)
  }

  override fun update(delta: Float) {
    this.delta = delta
    val worlds = worldsOrdered()
    worlds.forEach { it.preUpdate() }
    groupsOrdered().forEach {
      if (it.jobs.isNotEmpty())
        burstJobPool.execute(it.jobs)
    }
    worlds.forEach { it.postUpdate() }
  }

  override fun registerWorld(world: WorldManaged) {
    if (world in worldsToJob)
      return
    val group = groups[world.updateGroup]
        ?: throw IllegalStateException("update group not found: ${world.updateGroup}")
    val job = burstJobPool.createJobFor { world.update(delta) }
    worldsToJob[world] = job
    group.jobs += job
    worldsOrdered.clear()
  }

  override fun unregisterWorld(world: WorldManaged) {
    val job = worldsToJob.remove(world) ?: return
    val group = groups[world.updateGroup]
        ?: throw IllegalStateException("update group not found: ${world.updateGroup}")
    group.jobs -= job
    worldsOrdered.clear()
  }

  override fun start(engine: Engine) {
    burstJobPool.start()
  }

  override fun shutdown() {
    burstJobPool.shutdown()
  }
}