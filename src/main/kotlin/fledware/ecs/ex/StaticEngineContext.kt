package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineDataLifecycle
import fledware.ecs.util.MapperIndex
import fledware.utilities.get
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * There are optimizations that can happen if we allow static
 * [MapperIndex]. Plugins and extensions could access data much faster
 * and without a lookup.
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
 * reinitialized later, we set up tools (like the [StaticMapperIndex]) to
 * allow automatic (and lazy) reinitialization.
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
    engineHolder = null
    engine.events.onBeforeUpdate -= this::onBeforeUpdate
  }

  private fun onBeforeUpdate() {
    if (engineHolder !== engine) {
      onContextChangeLock.withLock {
        // ooo, fancy double check lock
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
 * This is meant to work with [StaticEngineContext] and try to read from the
 * staticContext every time. It will return immediately if the context is not set.
 *
 * It will attempt to initialize every call and sometimes may be initialized multiple
 * times. This shouldn't be an issue because the [onContextChange] method is idempotent.
 *
 * Example usage for a custom entity type:
 * <pre>{@code
 * val someStaticMapperIndex by StaticMapperIndex<ComponentType>()
 * val Entity.getComponentType: ComponentType
 *    get() {
 *      val index = someStaticMapperIndex ?: data.mapper.indexOf(ComponentType::class)
 *      return getOrNull(index)
 *    }
 * }</pre>
 */
class StaticMapperIndex<T : Any>(private val target: KClass<T>) {
  private var index: MapperIndex<T>? = null

  operator fun getValue(thisRef: Any?, property: KProperty<*>): MapperIndex<T>? {
    if (index != null) return index
    val engine = StaticEngineContext.engineHolder ?: return null
    val context = engine.data.contexts.get<StaticEngineContext>()
    context.addOnContextChange(this::onContextChange)
    index = engine.data.componentMapper.indexOf(target)
    return index
  }

  private fun onContextChange() {
    index = null
  }
}

/**
 *
 */
inline fun <reified T : Any> StaticMapperIndex() = StaticMapperIndex(T::class)
