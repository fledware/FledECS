package fledware.ecs.update

import java.util.concurrent.ExecutorService

/**
 * implementation of [BurstJobPool] that uses an [ExecutorService]
 * to execute its jobs.
 */
class BurstJobExecutorPool(val executor: ExecutorService,
                           val ownsExecutor: Boolean)
  : BurstJobPool {

  private val workLock = SimpleWorkLock()

  override fun createJobFor(block: () -> Unit) = BurstJob {
    workLock.executeAndCount(block)
  }

  override fun execute(jobs: List<BurstJob>) {
    workLock.reset(jobs.size)
    jobs.forEach { executor.execute(it.jobBlock) }
    workLock.awaitWork()
  }

  override fun start() {
  }

  override fun shutdown() {
    if (ownsExecutor)
      executor.shutdownNow()
  }
}
