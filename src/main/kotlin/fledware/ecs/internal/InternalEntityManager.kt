package fledware.ecs.internal

import fledware.ecs.Entity
import fledware.ecs.EntityManager
import fledware.ecs.OnEntityRemoved

interface InternalEntityManager : EntityManager {
  /**
   * remove the given entity from the world. This is generally called
   * when a world is transferring an [Entity] to another world.
   *
   * this will cause an [OnEntityRemoved] event.
   */
  fun remove(entity: Entity)
  /**
   * removes all entities from the given world.
   */
  fun removeEntities(): List<Entity>
  /**
   * deletes all entities from the given world
   */
  fun deleteEntities()
  /**
   * destroys this manager and quickly removes any owned resources.
   */
  fun destroy()
}
