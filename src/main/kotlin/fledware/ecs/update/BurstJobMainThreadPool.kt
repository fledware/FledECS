package fledware.ecs.update

/**
 * An implementation of [BurstJobPool] that just uses the
 * main thread to perform updates.
 */
class BurstJobMainThreadPool : BurstJobPool {
  override fun createJobFor(block: () -> Unit) = BurstJob(block)

  override fun execute(jobs: List<BurstJob>) {
    jobs.forEach { it.jobBlock() }
  }

  override fun start() {
  }

  override fun shutdown() {
  }
}