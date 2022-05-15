package fledware.ecs.update

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * this implementation of [BurstCyclicalJobPool] is most likely
 * the safest to use for concurrency. Every job that is added
 * is wrapped by a runnable to ensure context and exception handling.
 *
 */
class BurstCyclicalJobExecutorPool(val executor: ExecutorService,
                                   val ownsExecutor: Boolean = false)
  : BurstCyclicalJobPool {

  private var context: ClassLoader? = null

  private val exceptions = ConcurrentLinkedQueue<Throwable>()
  private val workWrappers = ConcurrentHashMap<() -> Unit, () -> Unit>()
  private val workLock = ReentrantLock()
  private var workCached: List<() -> Unit>? = null
  private val workArray: List<() -> Unit>
    get() = workCached ?: workLock.withLock {
      workCached = workWrappers.values.toList()
      workCached!!
    }

  private val workingLock = ReentrantLock()
  private val workingFinished = workingLock.newCondition()
  private val workingAt = AtomicInteger()
  private val workingRequired = AtomicInteger()

  override fun addJob(block: () -> Unit) {
    workWrappers[block] = {
      if (context != null)
        Thread.currentThread().contextClassLoader = context
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
    workCached = null
  }

  override fun removeJob(block: () -> Unit) {
    workWrappers -= block
    workCached = null
  }

  override fun execute() {
    val workArray = workArray
    workingAt.set(0)
    workingRequired.set(workArray.size)
    workArray.forEach { executor.execute(it) }
    workingLock.withLock {
      if (workingAt.get() < workingRequired.get())
        workingFinished.await()
    }
    if (exceptions.isNotEmpty()) {
      exceptions.forEach { it.printStackTrace() }
      throw IllegalStateException("unhandled exception in update thread")
    }
  }

  override fun setContext(context: ClassLoader) {
    this.context = context
  }

  override fun start() {
  }

  override fun shutdown() {
    if (ownsExecutor)
      executor.shutdownNow()
  }
}
