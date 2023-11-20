package fledware.ecs.internal

import fledware.ecs.EntityGroup

/**
 * the internal entity group that includes management methods
 */
interface InternalEntityGroup : EntityGroup {
  /**
   * Will remove all entities. This should only be used
   * for when all entities in this group is about to be removed.
   */
  fun clearEntities()
  /**
   * lets this group know to fire any events it may have.
   */
  fun fireEvents()
  /**
   * Will remove all entities and unsubscribe itself from any events.
   */
  fun destroy()
}
