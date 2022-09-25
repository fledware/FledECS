package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineDataLifecycle
import fledware.ecs.util.MapperIndex
import fledware.ecs.util.MapperList
import fledware.utilities.get
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * There are optimizations that can happen if we allow some static
 * data. For instance, having a static [MapperIndex] would allow plugins
 * and extensions could access data much faster and without a lookup.
 *
 * The main problem is where the data lives. If we store the indexes
 * in the engine context, then we'll have to do a hash lookup just
 * to get that context holding the index. In that case, it would be just as
 * fast as just doing a normal lookup for the [MapperIndex]. Static variables
 * don't work because if the classpath is reloaded (like in android),
 * we can't rely on the indexes staying in sync across reloads.
 *
 * This extension sets a static place for the engine to live before
 * every update. That way, if the ClassLoader is removed from memory and
 * reinitialized later, we set up tools (like the [StaticComponentMapperIndex]) to
 * allow automatic (and lazy) reinitialization.
 *
 * ** WARNING **
 * Do not use this if you expect to have multiple engines running at a time.
 */
fun Engine.withStaticEngineContext(): Engine {
  this.data.contexts.add(StaticEngineContext())
  return this
}

/**
 * This handles three major things:
 * - holds the static context for access externally
 * - checks if the engine changed before update
 * - sends an event if the engine changed.
 *
 * The alerts that are added via [addOnContextChange] are fired once, then
 * all the listeners are cleared. It is expected that the listeners put
 * itself into an initial state to be initialized lazily on the next read.
 */
class StaticEngineContext : EngineDataLifecycle {
  companion object {
    var engineHolder: Engine? = null
  }

  private lateinit var engine: Engine
  private val onContextChangeLock = ReentrantLock()
  private val onContextChange = mutableListOf<() -> Unit>()

  fun addOnContextChange(block: () -> Unit) = onContextChangeLock.withLock {
    onContextChange += block
  }

  override fun init(engine: Engine) {
    super.init(engine)
    this.engine = engine
    this.engine.events.onBeforeUpdate += this::onBeforeUpdate
  }

  override fun shutdown() {
    super.shutdown()
    engine.events.onBeforeUpdate -= this::onBeforeUpdate

    if (engineHolder !== null) {
      engineHolder = null
      onContextChange.forEach { it() }
      onContextChange.clear()
    }
  }

  private fun onBeforeUpdate() {
    if (engineHolder !== engine) {
      onContextChangeLock.withLock {
        // ooo, fancy double check lock...
        // It this call should never be called concurrently. But,
        // just to be extra careful because of the listeners.
        if (engineHolder !== engine) {
          onContextChange.forEach { it() }
          onContextChange.clear()
          engineHolder = engine
        }
      }
    }
  }
}

/**
 * This is the suggested pattern for creating static values.
 *
 * This is meant to work with [StaticEngineContext] and try to read from the
 * staticContext every time. It will return immediately if the context is not set.
 *
 * It will attempt to initialize every call and sometimes may be initialized multiple
 * times. This shouldn't be an issue because the [onContextChange] method is idempotent.
 *
 * The implementer only needs to override [getNewValue] to work.
 *
 * This abstract class handles a couple of important aspects:
 * - returns the value immediately if it is already set
 * - removes the value if the static context changes
 * - registers this value with [StaticEngineContext] when [getNewValue] return
 *   a non-null value.
 */
abstract class AbstractStaticValue<T: Any> {
  private var value: T? = null
  protected abstract fun getNewValue(engine: Engine): T?

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
    // if the value is already set, just return the value
    if (value != null) return value

    // get the engine. we want to always return null if the engine static
    // engine is null. The static engine should only be null if this is
    // called outside the update loop or if the engine is not configured
    // to use static values.
    val engine = StaticEngineContext.engineHolder ?: return null

    // return immediately if [getNewValue] returns null. else, register
    // this value with the static context, so we can remove this value if
    // the engine context changes.
    value = getNewValue(engine) ?: return null
    engine.data.contexts.get<StaticEngineContext>()
        .addOnContextChange(this::onContextChange)
    return value
  }

  private fun onContextChange() {
    value = null
  }
}



/**
 * A static implementation for a [MapperIndex] for entity components.
 *
 * Example usage for a custom entity type:
 * <pre>{@code
 * val someStaticMapperIndex by StaticComponentMapperIndex<ComponentType>()
 * val Entity.getComponentType: ComponentType
 *    get() {
 *      val index = someStaticMapperIndex ?: data.mapper.indexOf(ComponentType::class)
 *      return getOrNull(index)
 *    }
 * }</pre>
 */
class StaticComponentMapperIndex<T : Any>(private val target: KClass<T>)
  : AbstractStaticValue<MapperIndex<T>>() {
  override fun getNewValue(engine: Engine): MapperIndex<T> {
    return engine.data.componentMapper.indexOf(target)
  }
}

/**
 * Convenience method for creating a [StaticComponentMapperIndex]
 */
inline fun <reified T : Any> StaticComponentMapperIndex() = StaticComponentMapperIndex(T::class)

/**
 * Convenience method for using a static value.
 */
inline fun <reified T : Any> MapperList<Any, Any>.getOrFindIndex(index: MapperIndex<T>?) =
    index ?: mapper.indexOf(T::class)
