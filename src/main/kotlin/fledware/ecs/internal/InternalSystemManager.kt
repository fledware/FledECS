package fledware.ecs.internal

import fledware.ecs.System
import fledware.ecs.SystemManager

interface InternalSystemManager : SystemManager {
  /**
   * lets this manager know that mutations should be delayed
   * until [finishedSystemUpdateIteration] is called.
   *
   * this will return a snapshot of the systems in order
   * of [System.order].
   */
  fun startSystemUpdateIteration(): Array<System>

  /**
   * lets this manager know the update iteration is finished.
   */
  fun finishedSystemUpdateIteration()

  /**
   * removes all systems and releases any resources this manager has.
   */
  fun destroy()
}

fun InternalSystemManager.findSystemsOrdered(): Array<System> {
  try {
    return this.startSystemUpdateIteration()
  }
  finally {
    this.finishedSystemUpdateIteration()
  }
}

/**
 * a no allocation iteration of systems.
 */
inline fun InternalSystemManager.systemUpdateIteration(block: (system: System) -> Unit) {
  val systemsArray = this.startSystemUpdateIteration()
  try {
    for (index in systemsArray.indices) {
      block(systemsArray[index])
    }
  }
  finally {
    this.finishedSystemUpdateIteration()
  }
}
