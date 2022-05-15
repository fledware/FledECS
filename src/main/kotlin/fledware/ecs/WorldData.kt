package fledware.ecs

import fledware.ecs.util.MapperIndex
import fledware.utilities.MutableTypedMap
import fledware.utilities.TypedMap
import org.eclipse.collections.api.map.primitive.LongObjectMap
import kotlin.reflect.KClass

/**
 * The internal data of a world. The implementation
 * of World should pass this along to systems to access
 * data that only exists in the world the system belongs to.
 *
 * Assuming the systems need to update in series (the
 * default implementation), this can be accessed in any
 * way by systems.
 */
interface WorldData : EntityFactory {
  /**
   * the world that this data belongs to
   */
  val world: World

  /**
   * world level components. This is never cleared (except for on destroy)
   * and can be used for global data to this world.
   */
  val components: MutableTypedMap<Any>

  /**
   * all the systems in this world.
   */
  val systems: TypedMap<System>

  /**
   * all entities in the world
   */
  val entities: LongObjectMap<Entity>

  /**
   * all entities in this world that have a name
   */
  val entitiesNamed: Map<String, Entity>

  /**
   * built in groups that can be shared across systems
   */
  val entityGroups: Map<String, EntityGroup>

  /**
   * adds a system to this world data. This will automatically
   * enable the system.
   */
  fun addSystem(system: System)

  /**
   *
   */
  fun <S : System> removeSystem(key: KClass<S>)

  /**
   * creates a managed entity group.
   *
   * If no name is passed in, a unique name will be created. Either way,
   * the group will be added to `entityGroups` so they can be shared or
   * used in extensions.
   *
   * @param name the name of the group
   * @param include checks entity on if it should be included in the given group
   * @return the created entity group
   */
  fun createEntityGroup(name: String = "", include: (entity: Entity) -> Boolean): EntityGroup

  /**
   * removes an entity group with the given name.
   * This will not remove entities.
   */
  fun removeEntityGroup(name: String)

  /**
   * Removes the given entity group.
   * This will not remove entities.
   */
  fun removeEntityGroup(group: EntityGroup)

  /**
   * quickly clears _all_ entities without causing events or extra processing.
   *
   * This should not be called while updating.
   *
   * @return all the entities removed
   */
  fun clearEntities(): List<Entity>

  /**
   * removes the given entity from ownership of this world.
   *
   * The entity is not actually cleaned as is safe to reuse
   * after this method returns.
   */
  fun removeEntity(entity: Entity)

  /**
   * send an entity to the given world
   */
  fun sendEntity(world: String, entity: Entity)

  /**
   * send a message to the given world
   */
  fun sendMessage(world: String, message: Any)

  /**
   * Clears caches of data if there is any.
   *
   * This is useful for forcing reindexing of systems
   * or any extension data that needs to be reprocessed.
   */
  fun clearCaches()
}

/**
 * gets the MapperIndex for the given entity component
 */
fun <T : Any> WorldData.componentIndexOf(clazz: KClass<T>): MapperIndex<T> =
    engine.data.entityComponentIndexOf(clazz)

/**
 * gets the MapperIndex for the given entity component
 */
inline fun <reified T : Any> WorldData.componentIndexOf() =
    engine.data.entityComponentIndexOf(T::class)


inline fun <reified T : System> TypedMap<System>.get() = get(T::class)

inline fun <reified T : System> TypedMap<System>.getMaybe() = getMaybe(T::class)

inline fun <reified T : System> TypedMap<System>.getExact() = getExact(T::class)

inline fun <reified T : System> TypedMap<System>.getExactMaybe() = getExactMaybe(T::class)

