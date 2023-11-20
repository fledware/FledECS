package fledware.ecs.update

import fledware.ecs.util.OrderedValue

interface UpdateEngine {
  /**
   *
   */
  val groups: Map<Any, UpdateGroup>
  /**
   *
   */
  // TODO: maybe make this a builder? so we can add tasks that run before
  fun createUpdateGroup(key: Any, order: Int): UpdateGroup
  /**
   *
   */
  fun removeUpdateGroup(key: Any)
  /**
   * executes all of [UpdateGroup].
   */
  fun update()
  /**
   * Stops and cleans the engine.
   */
  fun shutdown()
}

interface UpdateNode : OrderedValue {
  /**
   *
   */
  val key: Any
  /**
   * cleans any resources specific to this [UpdateGroup].
   *
   * Generally, the [UpdateEngine] will call this method.
   */
  fun onRemoved()
}

/**
 * an update group can be executed within the [UpdateEngine] in
 * any order with groups.
 */
interface UpdateGroup : UpdateNode {
  /**
   *
   */
  fun registerJob(job: UpdateJob)
  /**
   *
   */
  fun removeJob(key: Any)
}

interface UpdateJob : UpdateNode {
  /**
   *
   */
  fun update()
}
