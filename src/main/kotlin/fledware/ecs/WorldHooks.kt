package fledware.ecs

import fledware.ecs.util.OrderedValue
import fledware.ecs.util.SortedList

/**
 * These are lower level hooks to directly change the behaviour of
 * the world. They are not designed to be event handlers.
 *
 * The handlers below are assumed to only be used in a single thread.
 */
interface WorldHooks {
  /**
   * called before a world begins the update process
   */
  val onBeforeUpdate: MutableList<(World) -> Unit>

  /**
   * called after a world finishes updating itself
   */
  val onAfterUpdate: MutableList<(World) -> Unit>

  /**
   * called when an entity is _deleted_... this is NOT called if an
   * entity is removed or passed to another world.
   */
  val onDeleteEntity: MutableList<(world: World, entity: Entity) -> Unit>

  /**
   * called when an entity is attempted to be created.
   *
   * There is a default hook for creating entities with a sort of MAX_VALUE
   * @see fledware.ecs.internal.DefaultEntityFactory
   * This hook will always be installed.
   *
   * the iteration of hooks will stop after the first non-null result.
   */
  val onCreateEntity: SortedList<OnCreateEntityWorldHook>

  /**
   * called during the [World.destroy] method, before anything is removed.
   */
  val onWorldDestroy: MutableList<(World) -> Unit>
}

interface OnCreateEntityWorldHook : OrderedValue {
  operator fun invoke(world: World): Entity?
}