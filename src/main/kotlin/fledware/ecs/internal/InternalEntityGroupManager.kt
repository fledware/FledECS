package fledware.ecs.internal

import fledware.ecs.EntityGroupManager

/**
 * the added internal methods of [EntityGroupManager]
 */
interface InternalEntityGroupManager: EntityGroupManager {
  /**
   * clears all entities from all groups.
   */
  fun clearEntities()
  /**
   *
   */
  fun fireEvents()
  /**
   * destroys all groups and clears any other resources it owns.
   */
  fun destroy()
}
