package fledware.ecs

interface Engine {
  /**
   * The update strategy for the engine.
   */
  val updateStrategy: EngineUpdateStrategy

  /**
   * Lifecycle events that are at the engine level.
   */
  val events: EngineEvents

  /**
   * All the data associated with this engine.
   *
   * This gives a logical separation for where extensions methods
   * should exist for engine extensions.
   */
  val data: EngineData

  /**
   * The options this engine is initialized with.
   */
  val options: EngineOptions

  /**
   * Whether this engine is started.
   *
   * If this returns false, [start] needs to be called.
   */
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
  fun addCreateWorldDecorator(decorator: WorldBuilderDecorator)

  /**
   * Requests a world to be created. Worlds cannot be created during updates
   * because it will cause concurrency complications. Instead, worlds can request
   * that other worlds be created. Only after the EngineUpdateStrategy has
   * finished will the engine then create a world.
   */
  fun requestCreateWorld(name: String, options: Any? = null, decorator: WorldBuilderDecorator)

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

  /**
   * Updates the active worlds based on the [updateStrategy].
   *
   * This method will automatically handle requests before and
   * after the update strategy finishes.
   */
  fun update(delta: Float)

  /**
   * Starts the engine. This should be called before update.
   */
  fun start()

  /**
   * Stops and cleans the engine.
   */
  fun shutdown()
}

/**
 * Sending an entity at the engine level. The given entity must not be
 * registered to a world. Generally, this method is meant to be used by
 * worlds.
 *
 * If you are trying to send an entity to a world within a system, use
 * [WorldData.sendEntity].
 *
 * The entity is guaranteed to be registered to the target world on
 * the beginning of the _next_ call to [Engine.update].
 */
fun Engine.sendEntity(worldName: String, entity: Entity) {
  check(entity.worldSafe == null) { "entity is owned by world: ${entity.worldSafe}" }
  val world = data.worlds[worldName]
    ?: throw IllegalStateException("world not found: $worldName")
  world.receiveEntity(entity)
}

/**
 * Sends a message to the target world.
 */
fun Engine.sendMessage(worldName: String, message: Any) {
  val world = data.worlds[worldName]
    ?: throw IllegalStateException("world not found: $worldName")
  world.receiveMessage(message)
}

/**
 * Convenience method for calling [Engine.requestCreateWorld] and immediately
 * handling the requests to force the creation of the world.
 */
fun Engine.createWorldAndFlush(name: String,
                               options: Any? = null,
                               decorator: WorldBuilderDecorator): World {
  requestCreateWorld(name, options, decorator)
  handleRequests()
  return data.worlds[name]!!
}
