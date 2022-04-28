package fledware.ecs.threads

import java.util.concurrent.Executors

/**
 * A job pool that doesn't remove any of its jobs after execution.
 */
interface BurstCyclicalJobPool {
  /**
   * Execute all jobs that have been added. Any exception within
   * a worker thread will cause an exception to be thrown once
   * the rest of the worker threads are finished.
   *
   * The caller thread will not return until all jobs are finished.
   *
   * The job pool is immutable during the [execute] call.
   */
  fun execute()
  /**
   * Add a job to be performed when [execute] is called.
   */
  fun addJob(block: () -> Unit)
  /**
   * Remove a job from being performed when [execute] is called.
   */
  fun removeJob(block: () -> Unit)
  /**
   * Sets the ClassLoader context for all the worker threads
   * when performing the added jobs during [execute].
   */
  fun setContext(context: ClassLoader)
  /**
   * Must be called before [execute].
   */
  fun start()
  /**
   * Cleanup any resources.
   */
  fun shutdown()
}

@Suppress("FunctionName")
fun BurstCyclicalJobPool(threads: Int = Runtime.getRuntime().availableProcessors()) =
    BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(threads), true)
