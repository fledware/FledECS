package fledware.ecs.update

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An abstract to coordinate the work with the main thread.
 */
class SimpleWorkLock {
  val exceptions = ConcurrentLinkedQueue<Throwable>()
  val workingLock = ReentrantLock()
  val workingFinished = workingLock.newCondition()!!
  val workingAt = AtomicInteger()
  val workingRequired = AtomicInteger()

  inline fun executeAndCount(block: () -> Unit) {
    try {
      block()
    }
    catch (ex: Throwable) {
      exceptions += ex
    }
    finally {
      if (workingAt.incrementAndGet() == workingRequired.get())
        workingLock.withLock { workingFinished.signalAll() }
    }
  }

  fun reset(size: Int) {
    workingAt.set(0)
    workingRequired.set(size)
  }

  fun awaitWork() {
    workingLock.withLock {
      if (workingAt.get() < workingRequired.get())
        workingFinished.await()
    }
    if (exceptions.isNotEmpty()) {
      exceptions.forEach { it.printStackTrace() }
      throw IllegalStateException("unhandled exception in update thread")
    }
  }
}