package fledware.ecs

/**
 * the update strategy of the entire engine.
 *
 * Important note:
 * Callers of this interface can assume (with the DefaultEngine)
 * that registerWorld, unregisterWorld and shutdown
 * will not be called during update.
 */
interface EngineUpdateStrategy {
  /**
   * This is generally called by the Engine (or a custom
   * update pattern by user), and should be finished with
   * all updating when the method returns.
   *
   * Unless there are concrete reasons, the pattern for world
   * updates should generally be:
   * - call preUpdate on all worlds
   * - call update on all worlds
   * - call postUpdate on all worlds
   */
  fun update(delta: Float)

  /**
   * creates a new world builder that will work with this update strategy
   */
  fun worldBuilder(engine: Engine, name: String, options: Any?): WorldBuilder

  /**
   * create and register an entity group with this update strategy
   */
  fun entityGroup(include: (entity: Entity) -> Boolean): EntityGroupManaged

  /**
   * lets the strategy know that a world should be updated.
   */
  fun registerWorld(world: WorldManaged)

  /**
   * lets the strategy know a world should no longer be updated.
   */
  fun unregisterWorld(world: WorldManaged)

  /**
   * sets the context used for any thread that is created
   */
  fun setThreadContext(context: ClassLoader)

  /**
   * lets the strategy know to initialize anything it needs
   * to start performing updates.
   */
  fun start(engine: Engine)

  /**
   * requires this to do any cleanup immediately.
   */
  fun shutdown()
}
