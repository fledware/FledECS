package fledware.ecs.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


interface EventListeners0 {
  operator fun plusAssign(listener: () -> Unit)
  operator fun minusAssign(listener: () -> Unit)
  fun add(listener: () -> Unit)
  fun remove(listener: () -> Unit)
}

class ImmediateEventListeners0 : EventListeners0 {
  val listeners = ConcurrentHashMap.newKeySet<() -> Unit>()!!
  override fun add(listener: () -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: () -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: () -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: () -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke() = exec { listeners.forEach { it() } }
}

class BufferedEventListeners0 : EventListeners0 {
  val buffer = AtomicBoolean(false)
  val listeners = ConcurrentHashMap.newKeySet<() -> Unit>()!!
  override fun add(listener: () -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: () -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: () -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: () -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke() = exec { buffer.set(false) }
  fun fire() {
    if (buffer.compareAndSet(true, false)) {
      listeners.forEach { it() }
    }
  }
}


interface EventListeners1<T> {
  operator fun plusAssign(listener: (T) -> Unit)
  operator fun minusAssign(listener: (T) -> Unit)
  fun add(listener: (T) -> Unit)
  fun remove(listener: (T) -> Unit)
}

class ImmediateEventListeners1<T>: EventListeners1<T> {
  val listeners = ConcurrentHashMap.newKeySet<(T) -> Unit>()!!
  override fun add(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke(input: T) = exec { listeners.forEach { it(input) } }
}

class BufferedEventListeners1<T>: EventListeners1<T> {
  val buffer = ConcurrentLinkedQueue<T>()
  val listeners = ConcurrentHashMap.newKeySet<(T) -> Unit>()!!
  override fun add(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override fun remove(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  override operator fun plusAssign(listener: (T) -> Unit) = exec { listeners.add(listener) }
  override operator fun minusAssign(listener: (T) -> Unit) = exec { listeners.remove(listener) }
  operator fun invoke(input: T) = exec { buffer.add(input) }
  fun removeEvent(input: T) = exec { buffer.remove(input) }
  fun fire() {
    buffer.forEach { event -> listeners.forEach { it(event) } }
    buffer.clear()
  }
}
