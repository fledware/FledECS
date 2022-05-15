@file:Suppress("MemberVisibilityCanBePrivate")

package fledware.ecs.impl

import fledware.ecs.Engine
import fledware.ecs.EngineDataInternal
import fledware.ecs.EngineDataLifecycle
import fledware.ecs.EngineEvents
import fledware.ecs.EngineOptions
import fledware.ecs.EngineUpdateStrategy
import fledware.ecs.Entity
import fledware.ecs.ManagedEntity
import fledware.ecs.World
import fledware.ecs.WorldBuilderDecorator
import fledware.ecs.WorldManaged
import fledware.ecs.util.ImmediateEventListeners1
import fledware.ecs.util.Mapper
import fledware.ecs.util.MapperIndex
import fledware.utilities.ConcurrentTypedMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass


/**
 * This implementation is thread safe and can
 * be accessed concurrently by worlds as updates happen.
 */
open class DefaultEngine(override val updateStrategy: EngineUpdateStrategy = mainThreadUpdateStrategy(),
                         override val data: EngineDataInternal = DefaultEngineData(),
                         override val options: EngineOptions = EngineOptions())
  : Engine {
  override val events = DefaultEngineEvents()
  private val worldDecorators = CopyOnWriteArrayList<WorldBuilderDecorator>()
  private val requests = ConcurrentLinkedDeque<Any>()
  private val mutating = AtomicBoolean(false)
  override var started: Boolean = false
    protected set

  private inline fun mutatingLock(block: () -> Unit) {
    if (!mutating.compareAndSet(false, true))
      throw IllegalStateException("cannot call while calling another method that mutates state")
    try {
      block()
    }
    finally {
      mutating.set(false)
    }
  }

  override fun update(delta: Float) = mutatingLock {
    actualHandleRequests()
    updateStrategy.update(delta)
    actualHandleRequests()
  }

  override fun start() = mutatingLock {
    if (started)
      throw IllegalStateException("already started")
    started = true
    updateStrategy.start(this)
    updateStrategy.createWorldUpdateGroup(options.defaultUpdateGroupName,
                                          options.defaultUpdateGroupOrder)
    data.start(this)
    events.onEngineStart(this)
    actualHandleRequests()
  }

  override fun shutdown() = mutatingLock {
    events.onEngineShutdown(this)
    requests.clear()
    events.clear()
    data.shutdown()
    updateStrategy.shutdown()
  }


  // ================================================================
  //
  // requests
  //
  // ================================================================

  override fun handleRequests() = mutatingLock {
    actualHandleRequests()
  }

  private fun actualHandleRequests() {
    while (true) {
      val request = requests.poll() ?: break
      when (request) {
        is EngineRequestWorldCreate -> createWorld(request)
        is EngineRequestWorldDestroy -> destroyWorld(request.worldName)
        is EngineRequestWorldUpdated -> updateWorld(request)
        is EngineRequestBlock -> safeBlock(request)
        else -> throw IllegalStateException("unexpected request: $request")
      }
    }
  }

  override fun addCreateWorldDecorator(decorator: WorldBuilderDecorator) {
    worldDecorators += decorator
  }

  override fun requestCreateWorld(name: String,
                                  options: Any?,
                                  decorator: WorldBuilderDecorator) {
    requests.add(EngineRequestWorldCreate(name, options, decorator))
  }

  override fun requestDestroyWorld(name: String) {
    requests.add(EngineRequestWorldDestroy(name))
  }

  override fun requestWorldUpdated(name: String, update: Boolean) {
    requests.add(EngineRequestWorldUpdated(name, update))
  }

  override fun requestSafeBlock(block: Engine.() -> Unit) {
    requests.add(EngineRequestBlock(block))
  }


  // ================================================================
  //
  //
  //
  // ================================================================

  private fun safeBlock(request: EngineRequestBlock) {
    request.block.invoke(this)
  }

  private fun createWorld(request: EngineRequestWorldCreate): World {
    val builder = updateStrategy.worldBuilder(this, request.worldName, request.options)
    worldDecorators.forEach { builder.it() }
    val decorator = request.builder
    builder.decorator()
    val result = builder.build()
    data.addWorld(result)
    if (options.autoWorldUpdateOnCreate)
      updateStrategy.registerWorld(result)
    result.onCreate()
    events.onWorldCreated(result)
    return result
  }

  private fun destroyWorld(name: String) {
    val world = data.removeWorld(name)
    updateStrategy.unregisterWorld(world)
    world.onDestroy()
    events.onWorldDestroyed(world)
  }

  private fun updateWorld(request: EngineRequestWorldUpdated) {
    val world = data.worlds[request.worldName]
        ?: throw IllegalStateException("world not found: $request")
    if (request.update)
      updateStrategy.registerWorld(world as WorldManaged)
    else
      updateStrategy.unregisterWorld(world as WorldManaged)
  }
}


// ==================================================================
//
// event objects used internally by DefaultEngine
//
// ==================================================================

data class EngineRequestWorldCreate(val worldName: String,
                                    val options: Any?,
                                    val builder: WorldBuilderDecorator)

data class EngineRequestWorldDestroy(val worldName: String)

data class EngineRequestWorldUpdated(val worldName: String,
                                     val update: Boolean)

data class EngineRequestBlock(val block: Engine.() -> Unit)

/**
 * Default implementation for [EngineEvents].
 *
 * This implementation is only immediate event listeners.
 */
class DefaultEngineEvents : EngineEvents {
  override val onEngineStart = ImmediateEventListeners1<Engine>()
  override val onEngineShutdown = ImmediateEventListeners1<Engine>()
  override val onWorldCreated = ImmediateEventListeners1<World>()
  override val onWorldDestroyed = ImmediateEventListeners1<World>()

  fun clear() {
    onEngineStart.listeners.clear()
    onEngineShutdown.listeners.clear()
    onWorldCreated.listeners.clear()
    onWorldDestroyed.listeners.clear()
  }
}

/**
 * Default implementation of EngineData
 *
 * This implementation is thread safe and fairly performant.
 * This class is also left open so engine developers can add
 * extensions to the engine without needing to change the
 * engine itself.
 */
open class DefaultEngineData : EngineDataInternal {
  protected val entityComponentMapper = Mapper<KClass<*>>()
  protected val entityIds = AtomicLong()
  override val worlds = ConcurrentHashMap<String, WorldManaged>()
  override val components = ConcurrentTypedMap()

  override fun <T : Any> entityComponentIndexOf(clazz: KClass<T>): MapperIndex<T> {
    return entityComponentMapper.indexOf(clazz)
  }

  override fun addWorld(world: WorldManaged) {
    if (worlds.putIfAbsent(world.name, world) != null)
      throw IllegalStateException("world already exists: ${world.name}")
  }

  override fun removeWorld(name: String): WorldManaged {
    val world = worlds.remove(name)
        ?: throw IllegalStateException("world doesn't exists: $name")
    world.onDestroy()
    return world
  }

  override fun createEntity(decorator: Entity.() -> Unit): Entity {
    val result = ManagedEntity(entityIds.incrementAndGet(), entityComponentMapper.list())
    result.decorator()
    return result
  }

  override fun start(engine: Engine) {
    components.values.forEach { (it as? EngineDataLifecycle)?.init(engine) }
  }

  override fun shutdown() {
    worlds.values.forEach { it.onDestroy() }
    worlds.clear()
    components.values.forEach { (it as? EngineDataLifecycle)?.shutdown() }
    components.clear()
  }
}

