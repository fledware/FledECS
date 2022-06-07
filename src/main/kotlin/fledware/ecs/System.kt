package fledware.ecs

/**
 * A system with no functionality that is managed by a world.
 */
interface System {
  /**
   * Lets the world know if this system is enabled and if
   * update should be called.
   */
  val enabled: Boolean
  /**
   * The order this system should be updated in.
   *
   * There is no guaranteed ordering for systems that have
   * the same order.
   *
   * The default order is 0 and can be changed during updates,
   * but it is up to the [World] implementor on how to handle
   * those changes and when they actually take effect.
   */
  val order: Int
  /**
   * Called by the world once the world is finished creating.
   *
   * @param world the world that is managing this system
   * @param data the world data that can be accessed by this system
   *             without data protections during update
   */
  fun onCreate(world: World, data: WorldData) = Unit
  /**
   * Called when the world no longer needs this system.
   */
  fun onDestroy() = Unit
  /**
   * Called on world update.
   *
   * @param delta the time since last update
   */
  fun update(delta: Float)
}

/**
 * A basic abstract system. this just holds world/data
 * in variables.
 */
abstract class AbstractSystem(enabled: Boolean = true, order: Int = 0) : System {
  /**
   * Implementation of enabled where [onEnabled] and [onDisabled] methods
   * are called. The setter does not check the previous value and will
   * always call [onEnabled] when set to true and [onDisabled] when set to false.
   *
   * [onEnabled] or [onDisabled] is called during [onCreate].
   */
  override var enabled: Boolean = enabled
    set(value) {
      field = value
      if (value)
        onEnabled()
      else
        onDisabled()
    }

  /**
   * The order of this world.
   *
   * When setting this value, it automatically calls [WorldData.clearCaches]
   * to force a reindex and sorting of the world systems.
   */
  override var order: Int = order
    set(value) {
      field = value
      dataSafe?.clearCaches()
    }

  private var engineSafe: Engine? = null
  private var worldSafe: World? = null
  private var dataSafe: WorldData? = null

  /**
   * the engine that manages this system
   */
  val engine: Engine get() = engineSafe
      ?: throw IllegalStateException("onCreate must be called first")

  /**
   * the world that manages this system
   */
  val world: World get() = worldSafe
      ?: throw IllegalStateException("onCreate must be called first")

  /**
   * The data of the world that manages this system.
   *
   * This allows access to [World.data] without the update
   * protection check.
   */
  val data: WorldData get() = dataSafe
      ?: throw IllegalStateException("onCreate must be called first")

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    this.engineSafe = world.engine
    this.worldSafe = world
    this.dataSafe = data
    if (enabled)
      onEnabled()
    else
      onDisabled()
  }

  override fun onDestroy() {
    super.onDestroy()
    if (enabled)
      enabled = false
  }

  /**
   * called when enabled is set to true.
   */
  open fun onEnabled() = Unit

  /**
   * called when enabled is set to false.
   */
  open fun onDisabled() = Unit
}

/**
 * A simple iterator system that just needs to update one
 * entity at a time in a single group.
 */
abstract class GroupIteratorSystem(val groupName: String = "") : AbstractSystem() {
  /**
   * return true if this entity should be included in the
   * [entities] group
   */
  abstract fun includeEntity(entity: Entity): Boolean
  /**
   * processes a single entity
   */
  abstract fun processEntity(entity: Entity, delta: Float)

  lateinit var entities: EntityGroup
    private set

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    entities = data.createEntityGroup(groupName, this::includeEntity)
  }

  override fun update(delta: Float) {
    entities.forEach { processEntity(it, delta) }
  }
}
