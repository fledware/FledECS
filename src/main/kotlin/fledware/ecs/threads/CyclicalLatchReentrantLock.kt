package fledware.ecs.threads

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An implementation of [CyclicalLatchLock] that uses a ReentrantLock
 * to handle state changes.
 */
class CyclicalLatchReentrantLock : CyclicalLatchLock {
  private val countDown = AtomicInteger(0)
  private val lock = ReentrantLock()
  private val finished = lock.newCondition()
  private val start = lock.newCondition()
  override val isFinished get() = countDown.get() <= 0
  override val isRunning get() = countDown.get() > 0

  @Volatile
  override var version: Long = 0
    private set

  override fun start(count: Int) {
    lock.withLock {
      ThreadDebug.log { "CyclicalLatchLock.start: $count" }
      if (!countDown.compareAndSet(0, count))
        throw IllegalStateException("already working")
      version++
      start.signalAll()
    }
  }

  override fun signal() {
    lock.withLock {
      val count = countDown.decrementAndGet()
      ThreadDebug.log { "CyclicalLatchLock.signal: $count" }
      if (count < 0)
        throw IllegalStateException("signal called too many times")
      if (count == 0)
        finished.signalAll()
    }
  }

  override fun awaitStart(lastVersion: Long) {
    ThreadDebug.log { "awaitStart()" }
    if (lastVersion != version && isRunning) return
    lock.withLock {
      if (lastVersion != version && isRunning) return
      start.await()
    }
  }

  override fun awaitFinished() {
    ThreadDebug.log { "awaitFinished()" }
    if (isFinished) return
    lock.withLock {
      if (isFinished) return
      finished.await()
    }
  }
}
