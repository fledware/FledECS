package fledware.ecs

import fledware.ecs.util.MapperIndex
import fledware.ecs.util.MapperList
import fledware.ecs.util.forEachNotNull
import kotlin.reflect.KClass

/**
 * The E in ECS
 *
 * This contains an id that is unique across the entire engine
 * and a bunch of components that are indexed the same across
 * the entire engine.
 */
@Suppress("UNCHECKED_CAST")
abstract class Entity(
    val id: Long,
    val data: MapperList<Any, Any>
) {
  init {
    data.onUpdate = { events?.onUpdate(this) }
  }

  protected var events: EntityEvents? = null

  /**
   * the world that his entity belongs to.
   */
  var worldSafe: String? = null
    protected set

  /**
   * The world that this entity belongs to.
   *
   * @throws IllegalStateException when this entity doesn't belong to a world
   */
  val world: String get() = worldSafe ?: throw IllegalStateException("not part of world")

  /**
   * The name that this entity has. An entity name must
   * be unique for the world it belongs to. It doesn't need
   * to be unique to the engine.
   *
   * When an entity is passed to another world, it will not automatically
   * remove its own name. An error will be thrown if the receiving world
   * already has an entity with that name.
   */
  var name: String = ""
    set(value) {
      field = value
      events?.onNameChange(this)
    }

  /**
   * If this entity has a name.
   */
  val hasName get() = name != ""

  /**
   * A system should call this if this entity needs to have a notification
   * sent to signal this entity has been updated.
   *
   * Update notifications are dedupe and will only be fired once after
   * a system update.
   */
  fun notifyUpdate() = events?.onUpdate(this)

  /**
   * Adds the given component.
   *
   * This will cause a component lookup.
   *
   * @throws IllegalStateException if the component already exists
   */
  fun <T : Any> add(component: T): T =
      data.setByKeyOrThrow(component::class, component) as T

  /**
   * Adds the given component
   *
   * @throws IllegalStateException if the component already exists
   */
  fun <T : Any> add(index: MapperIndex<T>, component: T): T =
      data.setByIndexOrThrow(index, component)

  /**
   * Gets the component for the given class.
   *
   * This will cause a component lookup.
   *
   * @throws IllegalStateException if the component doesn't exist.
   */
  operator fun <T : Any> get(type: KClass<T>): T =
      data.getByKey(type) as T

  /**
   * Gets the component for the given class.
   *
   * @throws IllegalStateException if the component doesn't exist
   */
  operator fun <T : Any> get(index: MapperIndex<T>): T =
      data.getByIndex(index)

  /**
   * Attempts to remove the given component. Returns the removed value if any.
   */
  fun <T : Any> remove(type: KClass<T>): T? =
      data.unsetByKey(type) as T?

  /**
   * Attempts to remove the given component. Returns the removed value if any.
   */
  fun <T : Any> remove(index: MapperIndex<T>): T? =
      data.unsetByIndex(index)

  /**
   * Gets the value or returns null.
   */
  fun <T : Any> getOrNull(type: KClass<T>): T? =
      data.getByKeyOrNull(type) as? T

  /**
   * Gets the value or returns null.
   */
  fun <T : Any> getOrNull(index: MapperIndex<T>): T? =
      data.getByIndexOrNull(index)

  /**
   * Gets the component or creates a new one with [block] if it doesn't exist.
   */
  fun <T : Any> getOrAdd(type: KClass<T>, block: () -> T): T =
      getOrNull(type) ?: set(block())

  /**
   * Gets the component or creates a new one with [block] if it doesn't exist.
   */
  fun <T : Any> getOrAdd(index: MapperIndex<T>, block: () -> T): T =
      getOrNull(index) ?: set(block())

  /**
   * Sets the component with the given index.
   *
   * @return the [component]
   */
  operator fun <T : Any> set(index: MapperIndex<T>, component: T?): T =
      data.setByIndex(index, component) as T

  /**
   * Sets the component.
   *
   * @return the [component]
   */
  fun <T : Any> set(component: T): T =
      component.also { data.setByKey(it::class, it) }

  /**
   * Checks if the given component exists.
   *
   * This will cause a component lookup.
   */
  operator fun <T : Any> contains(type: KClass<T>) =
      data.containsKey(type)

  /**
   * Checks if the given component exists.
   */
  operator fun <T : Any> contains(index: MapperIndex<T>) =
      data.containsIndex(index)

  override fun toString(): String {
    if (hasName)
      return "Entity($id){$name}"
    return "Entity($id)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Entity) return false
    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

/**
 * Gets the component or creates a new one with [block] if it doesn't exist.
 */
inline fun <reified T : Any> Entity.getOrAdd(noinline block: () -> T): T =
    this.getOrAdd(T::class, block)

/**
 * Gets the component for the given class.
 *
 * This will cause a component lookup.
 *
 * @throws IllegalStateException if the component doesn't exist.
 */
inline fun <reified T : Any> Entity.get(): T =
    this[T::class]

/**
 * Gets the component for the given class.
 *
 * This will cause a component lookup.
 */
inline fun <reified T : Any> Entity.getOrNull(): T? =
    this.getOrNull(T::class)

/**
 * Checks if the given component exists.
 *
 * This will cause a component lookup.
 */
inline fun <reified T : Any> Entity.contains(): Boolean =
    this.contains(T::class)

/**
 * Creates a string that represents the entity and all of its components.
 */
fun Entity.debugToString(startDepth: Int = 0): String {
  val result = StringBuilder()
  fun StringBuilder.prependDepth(plus: Int): StringBuilder {
    repeat(startDepth + plus) { result.append(" ") }
    return this
  }

  result.prependDepth(0).append(this.toString()).append(": ").append(worldSafe ?: "(no world)")
  var hasComponents = false
  this.data.forEachNotNull { _, value ->
    result.appendLine().prependDepth(2).append(value)
    hasComponents = true
  }
  if (!hasComponents)
    result.appendLine().prependDepth(2).append("*no components*")
  return result.toString()
}

/**
 * The methods for managing an entity.
 *
 * These methods should only be called by the world.
 */
class ManagedEntity(id: Long, components: MapperList<Any, Any>) : Entity(id, components) {

  fun registerToWorld(worldName: String, events: EntityEvents) {
    if (worldSafe != null || this.events != null)
      throw IllegalStateException("$this is already registered to world $worldSafe")
    this.worldSafe = worldName
    this.events = events
  }

  fun unregisterWorld() {
    this.worldSafe = null
    this.events = null
  }
}
