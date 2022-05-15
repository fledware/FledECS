@file:Suppress("MemberVisibilityCanBePrivate")

package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineData
import fledware.ecs.Entity
import fledware.ecs.WorldData
import fledware.ecs.getOrAdd
import fledware.ecs.getOrNull
import fledware.ecs.util.Mapper
import fledware.ecs.util.exec
import fledware.utilities.get
import java.util.BitSet


// ==================================================================
//
// engine level
//
// ==================================================================

/**
 * Used by the Engine creator to enable this entity flags extension.
 */
fun Engine.withEntityFlags(): Engine {
  data.components.put(EntityFlagsData())
  return this
}

/**
 * The engine data to ensure all flags have the same index.
 */
class EntityFlagsData {
  val mapper = Mapper<String>()
  fun indexOfFlag(name: String) = FlagIndex(name, mapper[name])
}


// ==================================================================
//
// world/system level
//
// ==================================================================

/**
 * Warning: DO NOT CREATE DIRECTLY!!
 * Create this with one of the flagIndexOf() methods. That will
 * ensure that all flags have the same index across the engine.
 *
 * Usage example:
 * <pre>{@code
 *  class SomeSystem : GroupIteratorSystem() {
 *    val someFlag by lazy { data.flagIndexOf("some") }
 *    ...
 *    override fun processEntity(entity: Entity, delta: Float) {
 *      // read flag
 *      if (someFlag in entity) {
 *        ... do stuffs
 *      }
 *      // set flag
 *      entity += someFlag
 *      // unset flag
 *      entity -= someFlag
 *    }
 *  }
 * }</pre>
 *
 * @param name the name of the flag.
 * @param index the common index of the flag for the given name.
 */
data class FlagIndex(val name: String, val index: Int)

/**
 * Creates a FlagIndex that will have the same index across the engine.
 *
 * @param name the name of the flag.
 */
fun EngineData.flagIndexOf(name: String) =
    components.get<EntityFlagsData>().indexOfFlag(name)

/**
 * Creates a FlagIndex that will have the same index across the engine.
 *
 * @param name the name of the flag.
 */
fun WorldData.flagIndexOf(name: String) =
    this.world.engine.data.components.get<EntityFlagsData>().indexOfFlag(name)


// ==================================================================
//
// entity level
//
// ==================================================================

/**
 * Component used on the Entity for flags.
 */
class EntityFlags : CachingComponent {
  val flags = BitSet()
  operator fun get(index: Int): Boolean = flags[index]
  operator fun contains(index: Int): Boolean = flags[index]
  operator fun plusAssign(index: Int) = exec { flags.set(index) }
  operator fun minusAssign(index: Int) = exec { flags.clear(index) }
  override fun reset() = flags.clear()
}

/**
 * Returns true if the flag is set.
 */
operator fun Entity.contains(index: FlagIndex) = flagContains(index)
operator fun Entity.plusAssign(index: FlagIndex) = flagSet(index)
operator fun Entity.minusAssign(index: FlagIndex) = flagClear(index)

fun Entity.flagContains(index: FlagIndex): Boolean {
  val flags = this.getOrNull<EntityFlags>() ?: return false
  return index.index in flags
}

fun Entity.flagSet(index: FlagIndex) {
  val flags = this.getOrAdd { EntityFlags() }
  if (!flags[index.index]) {
    flags += index.index
    notifyUpdate()
  }
}

fun Entity.flagClear(index: FlagIndex) {
  val flags = this.getOrNull<EntityFlags>() ?: return
  if (flags[index.index]) {
    flags -= index.index
    notifyUpdate()
  }
}

fun Entity.flagClearAll() {
  val flags = this.getOrNull<EntityFlags>() ?: return
  flags.reset()
  notifyUpdate()
}
