@file:Suppress("MemberVisibilityCanBePrivate")

package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineData
import fledware.ecs.Entity
import fledware.ecs.WorldData
import fledware.ecs.util.Mapper
import fledware.ecs.util.exec
import fledware.utilities.get
import java.util.*


// ==================================================================
//
// engine level
//
// ==================================================================

/**
 * Used by the Engine creator to enable this entity flags extension.
 */
fun Engine.withEntityFlags(): Engine {
  data.contexts.put(EntityFlagsData())
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
    contexts.get<EntityFlagsData>().indexOfFlag(name)

/**
 * Creates a FlagIndex that will have the same index across the engine.
 *
 * @param name the name of the flag.
 */
fun WorldData.flagIndexOf(name: String) =
    this.world.engine.data.contexts.get<EntityFlagsData>().indexOfFlag(name)


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
  operator fun get(index: FlagIndex): Boolean = flags[index.index]
  operator fun contains(index: FlagIndex): Boolean = flags[index.index]
  operator fun plusAssign(index: FlagIndex) = exec { flags.set(index.index) }
  operator fun minusAssign(index: FlagIndex) = exec { flags.clear(index.index) }
  override fun reset() = flags.clear()
}

/**
 * the static index that will be used for
 */
val entityFlagsIndex by StaticComponentMapperIndex<EntityFlags>()

/**
 * gets [EntityFlags] on the given entity or returns null
 */
val Entity.flagsOrNull: EntityFlags?
  get() = getOrNull(data.getOrFindIndex(entityFlagsIndex))

/**
 * gets [EntityFlags] or adds it, then returns the instance.
 */
val Entity.flagsOrAdd: EntityFlags
  get() = getOrAdd(data.getOrFindIndex(entityFlagsIndex)) { EntityFlags() }

/**
 * Returns true if the flag is set.
 */
operator fun Entity.contains(index: FlagIndex) = flagContains(index)

/**
 * Ensures the flag is set.
 *
 * Automatically calls [Entity.notifyUpdate] if the flag wasn't already set.
 */
operator fun Entity.plusAssign(index: FlagIndex) = flagSet(index)

/**
 * Ensures the flag is not set.
 *
 * Automatically calls [Entity.notifyUpdate] if the flag was set.
 */
operator fun Entity.minusAssign(index: FlagIndex) = flagClear(index)

/**
 * Returns true if the flag is set.
 */
fun Entity.flagContains(index: FlagIndex): Boolean {
  val flags = this.flagsOrNull ?: return false
  return index.index in flags
}

/**
 * Ensures the flag is set.
 *
 * Automatically calls [Entity.notifyUpdate] if the flag wasn't already set.
 */
fun Entity.flagSet(index: FlagIndex) {
  val flags = this.flagsOrAdd
  if (!flags[index.index]) {
    flags += index.index
    notifyUpdate()
  }
}

/**
 * Ensures the flag is not set.
 *
 * Automatically calls [Entity.notifyUpdate] if the flag was set.
 */
fun Entity.flagClear(index: FlagIndex) {
  val flags = this.flagsOrNull ?: return
  if (flags[index.index]) {
    flags -= index.index
    notifyUpdate()
  }
}

/**
 * Clears all flags.
 *
 * This will always call [Entity.notifyUpdate].
 */
fun Entity.flagClearAll() {
  val flags = this.flagsOrNull ?: return
  flags.reset()
  notifyUpdate()
}
