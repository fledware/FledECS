package fledware.ecs

import fledware.ecs.update.DefaultUpdateStrategy
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

interface Engine {
  val updateStrategy: EngineUpdateStrategy
  val events: EngineEvents
  val data: EngineData
  val options: EngineOptions
  val started: Boolean

  /**
   * Should be called automatically by the Engine implementor during
   * the update loop. But if some setup or requests need to
   * be performed outside the update loop, this can be called to
   * handle the request* methods.
   */
  fun handleRequests()

  /**
   * Add a decorator that is called before the decorator
   * passed into the create world request.
   */
  fun addCreateWorldDecorator(decorator: WorldBuilderLambda)

  /**
   * Requests a world to be created. Worlds cannot be created during updates
   * because it will cause concurrency complications. Instead, worlds can request
   * that other worlds be created. Only after the EngineUpdateStrategy has
   * finished will the engine then create a world.
   */
  fun requestCreateWorld(name: String, options: Any? = null, decorator: WorldBuilderLambda)

  /**
   * Same as requestCreateWorld, but for destroying it.
   */
  fun requestDestroyWorld(name: String)

  /**
   * changes the state of a world to be updated with EngineUpdateStrategy
   */
  fun requestWorldUpdated(name: String, update: Boolean)

  /**
   * Requests the execution of the block to happen outside the EngineUpdateStrategy
   * update loop.
   */
  fun requestSafeBlock(block: Engine.() -> Unit)

  fun update(delta: Float)

  fun start()
  fun shutdown()
}

fun Engine.sendEntity(worldName: String, entity: Entity) {
  check(entity.worldSafe == null) { "entity is owned by world: ${entity.worldSafe}" }
  val world = data.worlds[worldName]
    ?: throw IllegalStateException("world not found: $worldName")
  world.receiveEntity(entity)
}

fun Engine.sendMessage(worldName: String, message: Any) {
  val world = data.worlds[worldName]
    ?: throw IllegalStateException("world not found: $worldName")
  world.receiveMessage(message)
}

fun Engine.createWorldAndFlush(name: String,
                               options: Any? = null,
                               decorator: WorldBuilderLambda): World {
  requestCreateWorld(name, options, decorator)
  handleRequests()
  return data.worlds[name]!!
}


// ==================================================================
//
// DefaultEngine
// This implementation is thread safe and can
// be accessed concurrently by worlds as updates happen.
//
// ==================================================================

open class DefaultEngine(override val updateStrategy: EngineUpdateStrategy = DefaultUpdateStrategy(),
                         override val data: EngineDataInternal = ConcurrentEngineData(),
                         override val options: EngineOptions = EngineOptions())
  : Engine {
  override val events = ConcurrentEngineEvents()
  private val worldDecorators = CopyOnWriteArrayList<WorldBuilderLambda>()
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
    data.start(this)
    events.onEngineCreated(this)
    actualHandleRequests()
  }

  override fun shutdown() = mutatingLock {
    events.onEngineDestroyed(this)
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
    events.fire()
  }

  override fun addCreateWorldDecorator(decorator: WorldBuilderLambda) {
    worldDecorators += decorator
  }

  override fun requestCreateWorld(name: String,
                                  options: Any?,
                                  decorator: WorldBuilderLambda) {
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
