package fledware.ecs

import fledware.ecs.util.MapperIndex
import fledware.utilities.MutableTypedMap
import kotlin.reflect.KClass

/**
 * All the mutable state of an engine is held here. This
 * can store anything that is required for user specific
 * stuff to function.
 */
interface EngineData {
  /**
   * all the worlds for the given engage
   */
  val worlds: Map<String, World>

  /**
   * The shared components. This can be used for extensions
   * or game data. whatever needs to be global to the entire engine.
   */
  val components: MutableTypedMap<Any>

  /**
   * Creates an entity. The ID is guaranteed to be unique
   * to the engine.
   */
  fun createEntity(decorator: Entity.() -> Unit): Entity

  /**
   * Gets a MapperIndex for the given entity component class.
   * The index is guaranteed to be the same so all systems can
   * reference data consistently for all entities.
   */
  fun <T : Any> entityComponentIndexOf(clazz: KClass<T>): MapperIndex<T>
}

/**
 * iterates over each world in the engine.
 */
inline fun EngineData.forEachWorld(block: (world: World) -> Unit) =
    worlds.values.forEach(block)

/**
 * The EngineData that includes methods required for
 * the Engine. These methods should not be called by user
 * code, and only by internal engine code.
 */
interface EngineDataInternal : EngineData {

  /**
   * Adds a world to the EngineData
   */
  fun addWorld(world: WorldManaged)

  /**
   * Removes a world from the EngineData
   */
  fun removeWorld(name: String): WorldManaged

  /**
   * called when the engine is started
   */
  fun start(engine: Engine)

  /**
   * called when the engine is shutdown
   */
  fun shutdown()
}

/**
 * Gets a MapperIndex for the given entity component class.
 * The index is guaranteed to be the same so all systems can
 * reference data consistently for all entities.
 */
inline fun <reified T : Any> EngineData.entityComponentIndexOf() =
    entityComponentIndexOf(T::class)

/**
 * used when data set in the EngineData needs to be aware
 * of engine lifecycle events.
 */
interface EngineDataLifecycle {
  fun init(engine: Engine) = Unit
  fun shutdown() = Unit
}