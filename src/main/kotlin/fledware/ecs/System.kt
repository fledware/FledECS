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
abstract class AbstractSystem : System {
  override var enabled: Boolean = true
    set(value) {
      field = value
      if (value)
        onEnabled()
      else
        onDisabled()
    }

  /**
   * the engine that manages this system
   */
  lateinit var engine: Engine
    private set

  /**
   * the world that manages this system
   */
  lateinit var world: World
    private set
  /**
   * the data of the world that manages this system
   */
  lateinit var data: WorldData
    private set

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    this.engine = world.engine
    this.world = world
    this.data = data
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
