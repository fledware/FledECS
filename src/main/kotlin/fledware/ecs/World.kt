package fledware.ecs

import fledware.ecs.internal.EntityFactory
import fledware.ecs.util.EventBus
import fledware.utilities.MutableTypedMap
import fledware.utilities.get

/**
 * The public interface for the engine and other worlds to work with.
 * Depending on the implementation, all the methods should
 * be thread safe.
 */
interface World {
  /**
   * the unique name of this world within the engine
   */
  val name: String

  /**
   * world level components. This is never cleared (except for on destroy)
   * and can be used for global data to this world.
   */
  val contexts: MutableTypedMap<Any>

  /**
   * The low level hooks for modifying the behaviour of this world.
   * This likely doesn't need to be used.
   *
   * The world will always install the standard hook for creating entities
   */
  val hooks: WorldHooks

  /**
   *
   */
  val entities: EntityManager

  /**
   *
   */
  val entityGroups: EntityGroupManager

  /**
   *
   */
  val systems: SystemManager

  /**
   * The events that happen during the update of this world.
   *
   * These events are buffered/deduped and happen before/after every system update.
   */
  val updateEvents: EventBus

  /**
   * a count of how many times this world has been updated
   */
  val updateIndex: Long

  /**
   * send a message to this world in a thread safe way. The default
   * implementation buffers messages and does the actual handling later.
   */
  fun receiveMessage(message: Any)

  /**
   * sends an entity to this World in a thread safe way. The default
   * implementation buffers these and does the actual import later.
   */
  fun receiveEntity(entity: Entity)

  /**
   * will immediately delete all entities. This should not be called during
   * an update or by a [System].
   *
   * This will release all entities and allow any hooks to recycle
   * them (for instance, caching).
   */
  fun deleteEntities()

  /**
   * will immediately remove all entities. this should not be called during
   * an update or by a [System].
   *
   * The entities will be returned, so they can be loaded again. This will not
   * allow certain hooks to run (like caching).
   */
  fun removeEntities(): List<Entity>

  /**
   * used by the engine to signal this world is to never be updated
   * again and should be cleaned up and deleted.
   */
  fun destroy()

  /**
   *
   */
  fun update(delta: Float)
}

inline fun <reified T: Any> World.componentIndexOf() =
    contexts.get<EntityFactory>().mapper.indexOf<T>(T::class)
