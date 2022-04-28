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
class Entity(
    val id: Long,
    val components: MapperList<KClass<*>, Any>
) {
  /**
   * the world that his entity belongs to.
   */
  var worldSafe: String? = null

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
      notifyUpdate()
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
  fun notifyUpdate() = components.fireOnUpdate()

  /**
   * Adds the given component.
   *
   * This will cause a component lookup.
   *
   * @throws IllegalStateException if the component already exists
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> add(component: T): T = components.setOrThrow(component::class, component) as T

  /**
   * Adds the given component
   *
   * @throws IllegalStateException if the component already exists
   */
  fun <T : Any> add(index: MapperIndex<T>, component: T): T = components.setOrThrow(index, component)

  /**
   * Gets the component for the given class.
   *
   * This will cause a component lookup.
   *
   * @throws IllegalStateException if the component doesn't exist.
   */
  @Suppress("UNCHECKED_CAST")
  operator fun <T : Any> get(type: KClass<T>): T = components[type] as T

  /**
   * Gets the component for the given class.
   *
   * @throws IllegalStateException if the component doesn't exist
   */
  operator fun <T : Any> get(index: MapperIndex<T>): T = components[index]

  /**
   * Attempts to remove the given component. Returns the removed value if any.
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> remove(type: KClass<T>): T? = components.unset(type) as T?

  /**
   * Attempts to remove the given component. Returns the removed value if any.
   */
  fun <T : Any> remove(index: MapperIndex<T>): T? = components.unset(index)

  /**
   * Gets the value or returns null.
   */
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> getOrNull(type: KClass<T>): T? = components.getOrNull(type) as? T

  /**
   * Gets the value or returns null.
   */
  fun <T : Any> getOrNull(index: MapperIndex<T>): T? = components.getOrNull(index)

  /**
   * Gets the component or creates a new one with [block] if doesn't exist.
   */
  fun <T : Any> getOrAdd(type: KClass<T>, block: () -> T): T {
    return getOrNull(type) ?: set(block())
  }

  /**
   * Gets the component or creates a new one with [block] if doesn't exist.
   */
  fun <T : Any> getOrAdd(index: MapperIndex<T>, block: () -> T): T {
    return getOrNull(index) ?: set(block())
  }

  operator fun <T : Any> set(index: MapperIndex<T>, component: T?): T? = components.set(index, component)

  fun <T : Any> set(component: T): T = component.also { components[it::class] = it }

  /**
   * Checks if the given component exists.
   *
   * This will cause a component lookup.
   */
  operator fun <T : Any> contains(type: KClass<T>) = type in components

  /**
   * Checks if the given component exists.
   */
  operator fun <T : Any> contains(index: MapperIndex<T>) = index in components

  override fun toString(): String {
    if (hasName)
      return "Entity($id){$name}"
    return "Entity($id)"
  }
}

inline fun <reified T : Any> Entity.getOrAdd(block: () -> T): T {
  return getOrNull(T::class) ?: set(block())
}

/**
 * Gets the component for the given class.
 *
 * This will cause a component lookup.
 *
 * @throws IllegalStateException if the component doesn't exist.
 */
inline fun <reified T : Any> Entity.get(): T = this[T::class]

/**
 * Gets the component for the given class.
 *
 * This will cause a component lookup.
 */
inline fun <reified T : Any> Entity.getOrNull(): T? = this.getOrNull(T::class)

/**
 * Creates a string that represents the entity and all of its components.
 */
fun Entity.debugToString(startDepth: Int = 0): String {
  val result = StringBuilder()
  fun StringBuilder.prependDepth(plus: Int): StringBuilder {
    repeat(startDepth + plus) { result.append(" ") }
    return this
  }

  result.prependDepth(0).append("Entity(").append(this.id).append(") ").append(this.name)
  var hasComponents = false
  this.components.forEachNotNull { _, value ->
    result.appendLine().prependDepth(2).append(value)
    hasComponents = true
  }
  if (!hasComponents)
    result.prependDepth(2).append("*no components*")
  return result.toString()
}
