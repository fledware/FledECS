package fledware.ecs.update

/**
 * An implementation of [BurstJobPool] that just uses the
 * main thread to perform updates.
 */
class BurstJobMainThreadPool : BurstJobPool {
  private var updateContext = Thread.currentThread().contextClassLoader

  override fun createJobFor(block: () -> Unit) = BurstJob(block)

  override fun execute(jobs: List<BurstJob>) {
    val context = Thread.currentThread().contextClassLoader
    try {
      Thread.currentThread().contextClassLoader = updateContext
      jobs.forEach { it.jobBlock() }
    }
    finally {
      Thread.currentThread().contextClassLoader = context
    }
  }

  override fun setContext(context: ClassLoader) {
    updateContext = context
  }

  override fun start() {
  }

  override fun shutdown() {
  }
}