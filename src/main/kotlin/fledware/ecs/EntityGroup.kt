package fledware.ecs

import fledware.ecs.util.EventListeners0

/**
 *
 */
interface EntityGroup {
  /**
   * the world this entity group is part of
   */
  val world: World
  /**
   * the entities in this group
   */
  val entities: List<Entity>
  /**
   * the amount of entities in the group
   */
  val size get() = entities.size
  /**
   * fired when an entity is added or removed
   */
  val onChange: EventListeners0
  /**
   * if the entity is in the group
   */
  operator fun contains(entity: Entity): Boolean
  /**
   *
   */
  operator fun iterator(): Iterator<Entity>
}

/**
 * the internal entity group that includes management methods
 */
interface EntityGroupManaged: EntityGroup {
  /**
   *
   */
  fun attachWorld(world: World, data: WorldData)
  /**
   * Will remove all entities. This should only be used
   * for when all entities in this group is about to be removed.
   */
  fun clear()
  /**
   * Will remove all entities and unsubscribe itself from any events.
   */
  fun finished()
}

inline fun EntityGroup.forEach(block: (entity: Entity) -> Unit) = entities.forEach(block)
