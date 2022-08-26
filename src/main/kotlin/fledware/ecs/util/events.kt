package fledware.ecs.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean


interface EventListeners0 {
  operator fun plusAssign(listener: () -> Unit)
  operator fun minusAssign(listener: () -> Unit)
  fun add(listener: () -> Unit)
  fun remove(listener: () -> Unit)
}

class ImmediateEventListeners0 : EventListeners0 {
  val listeners = CopyOnWriteArrayList<() -> Unit>()
  override fun add(listener: () -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: () -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: () -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: () -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke() = exec { listeners.forEach { it() } }
  fun clear() = exec { listeners.clear() }
}

class BufferedEventListeners0 : EventListeners0 {
  val buffer = AtomicBoolean(false)
  val listeners = CopyOnWriteArrayList<() -> Unit>()
  @Volatile
  var freezeFire = false
  override fun add(listener: () -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: () -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: () -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: () -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke() = exec { buffer.set(false) }
  fun clear() = exec { listeners.clear() }
  fun fire() {
    if (freezeFire)
      return
    if (buffer.compareAndSet(true, false)) {
      listeners.forEach { it() }
    }
  }
}


interface EventListeners1<T : Any> {
  operator fun plusAssign(listener: (T) -> Unit)
  operator fun minusAssign(listener: (T) -> Unit)
  fun add(listener: (T) -> Unit)
  fun remove(listener: (T) -> Unit)
}

class ImmediateEventListeners1<T : Any>: EventListeners1<T> {
  val listeners = CopyOnWriteArrayList<(T) -> Unit>()
  override fun add(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke(input: T) = exec { listeners.forEach { it(input) } }
  fun clear() = exec { listeners.clear() }
}

class BufferedEventListeners1<T : Any>: EventListeners1<T> {
  val buffer = UniqueList<T>()
  val listeners = CopyOnWriteArrayList<(T) -> Unit>()
  override fun add(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke(input: T) = exec { buffer.add(input) }
  fun removeEvent(input: T) = exec { buffer.remove(input) }
  fun clear() = exec { buffer.clear(); listeners.clear() }
  fun fire() {
    while (true) {
      val event = buffer.removeLastOrNull() ?: break
      listeners.forEach { it(event) }
    }
  }
}
