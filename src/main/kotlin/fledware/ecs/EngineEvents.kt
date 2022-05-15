package fledware.ecs

import fledware.ecs.util.EventListeners1


/**
 * Engine wide events
 */
interface EngineEvents {
  /**
   * Fired when [Engine.start] is called.
   */
  val onEngineStart: EventListeners1<Engine>
  /**
   * Fired when [Engine.shutdown] is called.
   */
  val onEngineShutdown: EventListeners1<Engine>
  /**
   * Called when a world is created.
   */
  val onWorldCreated: EventListeners1<World>
  /**
   * Called when a world is destroyed.
   */
  val onWorldDestroyed: EventListeners1<World>
}
