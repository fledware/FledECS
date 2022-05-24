package fledware.ecs.update

/**
 * The pool used to create and execute jobs.
 */
interface BurstJobPool {
  fun createJobFor(block: () -> Unit): BurstJob
  fun execute(jobs: List<BurstJob>)
  fun start()
  fun shutdown()
}

/**
 * A job created by the [BurstJobPool].
 *
 * Only use jobs created from the [BurstJobPool] on that
 * same instance.
 */
interface BurstJob {
  val jobBlock: () -> Unit
}

/**
 * A simple implementation of [BurstJob] that just
 * executes the block passed in.
 */
fun BurstJob(block: () -> Unit) = object : BurstJob {
  override val jobBlock: () -> Unit = block
}
