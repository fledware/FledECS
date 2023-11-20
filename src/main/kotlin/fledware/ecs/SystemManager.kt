package fledware.ecs

import fledware.utilities.TypedMap
import kotlin.reflect.KClass

/**
 * the user interface for systems
 */
interface SystemManager {
  /**
   * the amount of managed systems
   */
  val size: Int
  /**
   * all the systems
   */
  val systems: TypedMap<System>
  /**
   * the world this manager is for
   */
  val world: World
  /**
   * adds a system.
   *
   * If a system calls this during an update to the [World], then
   * this will not take effect immediately.
   */
  fun add(system: System)
  /**
   * removes the given system.
   *
   * If a system calls this during an update to the [World], then
   * this will not take effect immediately.
   *
   * Note, this removes based on how [TypedMap] removes objects.
   */
  fun remove(key: KClass<out System>)
}

inline fun <reified T : System> TypedMap<System>.get() = get(T::class)

inline fun <reified T : System> TypedMap<System>.getOrNull() = getOrNull(T::class)

inline fun <reified T : System> TypedMap<System>.getExact() = getExact(T::class)

inline fun <reified T : System> TypedMap<System>.getExactOrNull() = getExactOrNull(T::class)
