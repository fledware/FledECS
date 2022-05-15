package fledware.ecs.update

/**
 * A reusable latch lock meant to coordinate a single "starter"
 * thread with many "worker" threads.
 *
 * To ensure there is no thread thrashing when there is no more
 * work to start, but the started work has not finished, a version
 * is used. The version will be incremented every time [start] is
 * called. That version should be handed into the [awaitStart] so
 * threads can check that they are waiting on work to finish that
 * they in fact cannot help with.
 */
interface CyclicalLatchLock {
  /**
   * If this latch is considered open.
   */
  val isRunning: Boolean
  /**
   * If this latch is considered closed.
   */
  val isFinished: Boolean
  /**
   * The current version of work, regardless of open/closed state.
   */
  val version: Long
  /**
   * The implementor must change the state in three ways:
   * - set the latch state to open
   * - configure how many times [signal] must be called to be considered closed
   * - ensure the work threads are in an active state
   *
   * The caller to this method will return immediately.
   *
   * @param count the amount of times [signal] must be called to close latch
   */
  fun start(count: Int)
  /**
   * Called by a worker thread to let this latch know there is no
   * more work for them to do.
   *
   * The calling thread is responsible for closing the latch if this
   * is the nth time called from the [start] count argument. If the latch
   * is set to closed, the caller must also ensure any caller to
   * [awaitFinished] is in an active state.
   */
  fun signal()
  /**
   * Called by worker threads to await/wait/park the current thread until
   * [start] is called.
   *
   * This method should return immediately if [lastVersion] != [version],
   * because that means the expected [start] iteration that the given
   * thread is on is not the expected one.
   */
  fun awaitStart(lastVersion: Long)
  /**
   * Lets the caller of [start] to wait for the final [signal] call.
   */
  fun awaitFinished()
}