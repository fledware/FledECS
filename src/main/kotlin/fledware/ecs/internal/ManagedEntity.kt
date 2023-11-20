package fledware.ecs.internal

import fledware.ecs.Entity
import fledware.ecs.World
import fledware.ecs.util.MapperList

/**
 * The methods for managing an entity.
 *
 * These methods should only be called by the world.
 */
class ManagedEntity(id: String, components: MapperList<Any, Any>) : Entity(id, components) {

  fun registerToWorld(world: World) {
    if (worldIn != null)
      throw IllegalStateException("$this is already registered to world $worldSafe")
    this.worldIn = world
  }

  fun unregisterWorld(): Boolean {
    val result = this.worldIn != null
    this.worldIn = null
    return result
  }
}
