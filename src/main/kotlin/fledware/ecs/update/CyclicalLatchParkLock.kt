package fledware.ecs.update

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

/**
 * A [CyclicalLatchLock] that doesn't use any locks or monitor checks.
 *
 * A quick warning... This is a lock-less algorithm and is incredibly
 * difficult to test. It is not battle hardened or tested extensively
 * is production. Even though it is tested extensively with jmh and must
 * do 10Ms of iterations to complete a benchmark, it still should be
 * considered bleeding edge.
 */
class CyclicalLatchParkLock : CyclicalLatchLock {
  private val countDown = AtomicInteger(0)
  private val awaitingStartThreads = ConcurrentLinkedDeque<Thread>()
  override val isFinished get() = countDown.get() <= 0
  override val isRunning get() = countDown.get() > 0

  private val starter = AtomicReference<Thread>(null)
  @Volatile
  override var version: Long = 0
    private set

  override fun start(count: Int) {
    ThreadDebug.log { "CyclicalLatchLock.start: $count" }
    if (!countDown.compareAndSet(0, count))
      throw IllegalStateException("already working")
    version++
    awaitingStartThreads.forEach { LockSupport.unpark(it) }
  }

  override fun signal() {
    val count = countDown.decrementAndGet()
    ThreadDebug.log { "CyclicalLatchLock.signal: $count" }
    if (count < 0)
      throw IllegalStateException("signal called too many times")
    if (count == 0)
      LockSupport.unpark(starter.get())
  }

  override fun awaitStart(lastVersion: Long) {
    ThreadDebug.log { "awaitStart($lastVersion)" }
    awaitingStartThreads += Thread.currentThread()
    while (lastVersion == version || !isRunning) {
      LockSupport.park()
      if (Thread.interrupted())
        break
    }
    awaitingStartThreads -= Thread.currentThread()
  }

  override fun awaitFinished() {
    ThreadDebug.log { "awaitFinished()" }
    if (!starter.compareAndSet(null, Thread.currentThread()))
      throw IllegalStateException("start thread invalid: already set")
    while (!isFinished) {
      LockSupport.park()
      if (Thread.interrupted())
        break
    }
    if (!starter.compareAndSet(Thread.currentThread(), null))
      throw IllegalStateException("start thread invalid: not current thread")
  }
}