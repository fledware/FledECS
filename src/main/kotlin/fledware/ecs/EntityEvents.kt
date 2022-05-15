package fledware.ecs

/**
 * Entity events are for internal management.
 *
 * Generally, the world should buffer and coordinate when
 * events are actually fired to protect concurrency.
 */
interface EntityEvents {
  fun onNameChange(entity: Entity)
  fun onUpdate(entity: Entity)
}

