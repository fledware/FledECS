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
   * the name of this group
   */
  val name: String
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
 * iterates over all the given entities.
 */
inline fun EntityGroup.forEach(block: (entity: Entity) -> Unit) {
  for (index in entities.indices) {
    block(entities[index])
  }
}
