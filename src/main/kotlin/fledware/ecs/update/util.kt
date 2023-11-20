package fledware.ecs.update

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SimpleLock {
  val locked = AtomicBoolean(false)
  inline fun <T> withLock(block: () -> T): T {
    if (!locked.compareAndSet(false, true))
      throw IllegalStateException("Already locked. Only use these APIs by a single thread.")
    try {
      return block()
    }
    finally {
      locked.set(false)
    }
  }
}

class CounterWorkLink {
  val exceptions = ConcurrentLinkedQueue<Throwable>()
  val workingAt = AtomicInteger()
  val workingRequired = AtomicInteger()
  var onFinish: (() -> Unit)? = null

  inline fun executeAndCount(block: () -> Unit) {
    try {
      block()
    }
    catch (ex: Throwable) {
      exceptions += ex
    }
    finally {
      workFinished()
    }
  }

  fun workFinished() {
    if (workingAt.incrementAndGet() == workingRequired.get())
      onFinish?.invoke()
  }

  fun addWork(count: Int) {
    workingRequired.getAndAdd(count)
  }

  fun reset(size: Int, onFinish: (() -> Unit)? = null) {
    workingAt.set(0)
    workingRequired.set(size)
    this.onFinish = onFinish
  }
}
