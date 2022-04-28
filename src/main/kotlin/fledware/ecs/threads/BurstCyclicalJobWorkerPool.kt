package fledware.ecs.threads

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 */
class BurstCyclicalJobWorkerPool(private val concurrency: Int,
                                 private val latch: CyclicalLatchLock)
  : BurstCyclicalJobPool {

  private inner class WorkThread(name: String) : Thread(name) {
    @Volatile
    var running = true
    override fun run() {
      var lastWorkVersion = latch.version
      try {
        latch.signal()
        while (running) {
          latch.awaitStart(lastWorkVersion)
          if (lastWorkVersion != latch.version) {
            try {
              lastWorkVersion = latch.version
              performWorkUntilOverflow()
            }
            finally {
              latch.signal()
            }
          }
        }
      }
      catch (ex: Throwable) {
        if (ex !is InterruptedException) {
          ThreadDebug.log { "adding exception and exiting: $ex" }
          exceptions.add(ex)
        }
      }
    }
  }

  private fun performWorkUntilOverflow() {
    ThreadDebug.log { "performWorkUntilOverflow" }
    val workArray = workArray
    while (true) {
      val index = workAt.getAndIncrement()
      if (index >= workArray.size) break
      workArray[index]()
    }
  }

  private val executing = AtomicBoolean(false)
  private val exceptions = ConcurrentLinkedQueue<Throwable>()
  private val pool = Array(concurrency) { WorkThread("WorkerBurstCyclicalJobPool-$it") }
  private val work = ConcurrentHashMap.newKeySet<() -> Unit>()
  private val workAt = AtomicInteger()
  private var workCached: Array<() -> Unit>? = null
  private val workArray: Array<() -> Unit>
    get() = workCached ?: run {
      workCached = work.toTypedArray()
      workCached!!
    }

  private var started = false

  override fun execute() {
    ThreadDebug.log { "execute() enter" }
    if (!started)
      throw IllegalStateException("not started")
    assertNoExceptions()
    if (!executing.compareAndSet(false, true))
      throw IllegalStateException("already executing")

    try {
      workAt.set(0)
      latch.start(concurrency)
      latch.awaitFinished()

      assertNoExceptions()
      ThreadDebug.log { "execute() exit" }
    }
    finally {
      executing.set(false)
    }
  }

  private fun assetNotExecuting() {
    if (executing.get())
      throw IllegalStateException("pool is executing")
  }

  private fun assertNoExceptions() {
    if (exceptions.isNotEmpty()) {
      exceptions.forEach { it.printStackTrace() }
      throw IllegalStateException("unhandled exception in update thread")
    }
  }

  override fun addJob(block: () -> Unit) {
    ThreadDebug.log { "addJob()" }
    assetNotExecuting()
    if (!work.add(block))
      throw IllegalStateException("block already exists")
    workCached = null
  }

  override fun removeJob(block: () -> Unit) {
    ThreadDebug.log { "removeJob()" }
    assetNotExecuting()
    work.remove(block)
    workCached = null
  }

  override fun setContext(context: ClassLoader) {
    ThreadDebug.log { "setContext()" }
    assetNotExecuting()
    pool.forEach { it.contextClassLoader = context }
  }

  override fun start() {
    ThreadDebug.log { "start()" }
    assetNotExecuting()
    latch.start(concurrency)
    pool.forEach { it.start() }
    latch.awaitFinished()
    started = true
  }

  override fun shutdown() {
    ThreadDebug.log { "shutdown()" }
    assetNotExecuting()
    pool.forEach { it.running = false }
    pool.forEach { it.interrupt() }
    pool.forEach { it.join() }
  }
}
