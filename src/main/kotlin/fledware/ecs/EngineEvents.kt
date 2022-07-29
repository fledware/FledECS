package fledware.ecs

import fledware.ecs.util.EventListeners0
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
  /**
   * called before the update is passed to the [EngineUpdateStrategy]
   */
  val onBeforeUpdate: EventListeners0
  /**
   * Called after the update returns from [EngineUpdateStrategy]
   */
  val onAfterUpdate: EventListeners0
}
