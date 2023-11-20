package fledware.ecs.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


fun interface Subscription {
  fun remove()
}

interface EventBus {
  fun <E : Any> add(eventType: KClass<E>, listener: (event: E) -> Unit): Subscription
  fun clearListeners()
  fun fire(event: Any)
}

inline fun <reified E : Any> EventBus.add(
    noinline listener: (event: E) -> Unit
) = add(E::class, listener)

abstract class AbstractEventBus : EventBus {
  protected abstract val listeners: MutableMap<KClass<*>, MutableSet<(Any) -> Unit>>

  protected abstract fun createListenerSet(): MutableSet<(Any) -> Unit>

  override fun <E : Any> add(eventType: KClass<E>, listener: (event: E) -> Unit): Subscription {
    @Suppress("UNCHECKED_CAST")
    listener as (Any) -> Unit
    val listenersForType = listeners.compute(eventType) { _, check ->
      val listenersForType = check ?: createListenerSet()
      if (!listenersForType.add(listener))
        throw IllegalStateException("listener already listening on $eventType")
      listenersForType
    } as MutableSet
    return Subscription { listenersForType.remove(listener) }
  }

  override fun clearListeners() {
    listeners.clear()
  }
}

abstract class AbstractImmediateEventBus : AbstractEventBus() {
  override fun fire(event: Any) {
    val listenersForType = listeners[event::class] ?: return
    listenersForType.forEach { it(event) }
  }
}

class DefaultImmediateEventBus : AbstractImmediateEventBus() {
  override val listeners: MutableMap<KClass<*>, MutableSet<(Any) -> Unit>> = mutableMapOf()
  override fun createListenerSet(): MutableSet<(Any) -> Unit> = mutableSetOf()
}

class ConcurrentImmediateEventBus : AbstractImmediateEventBus() {
  override val listeners: MutableMap<KClass<*>, MutableSet<(Any) -> Unit>> = ConcurrentHashMap()
  override fun createListenerSet(): MutableSet<(Any) -> Unit> = ConcurrentHashMap.newKeySet()
}

interface BufferedEventBus : EventBus {
  fun fireBufferedEvents()

  fun clearBufferedEvents()
}

open class DefaultBufferedEventBus : AbstractEventBus(), BufferedEventBus {
  override val listeners: MutableMap<KClass<*>, MutableSet<(Any) -> Unit>> = mutableMapOf()
  override fun createListenerSet(): MutableSet<(Any) -> Unit> = mutableSetOf()

  protected val buffer = UniqueList<Any>()

  override fun fireBufferedEvents() {
    buffer.drainEach { event ->
      val listenersForEvent = listeners[event::class] ?: return@drainEach
      listenersForEvent.forEach { it(event) }
    }
  }

  override fun clearBufferedEvents() {
    buffer.clear()
  }

  override fun fire(event: Any) {
    buffer += event
  }
}
