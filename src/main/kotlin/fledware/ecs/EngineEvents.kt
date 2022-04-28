package fledware.ecs

import fledware.ecs.util.BufferedEventListeners1
import fledware.ecs.util.EventListeners1
import fledware.ecs.util.ImmediateEventListeners1

// ==================================================================
//
// engine wide events
//
// ==================================================================

interface EngineEvents {
  /**
   * called when a world is created
   */
  val onEngineCreated: EventListeners1<Engine>
  /**
   * called when a world is created
   */
  val onEngineDestroyed: EventListeners1<Engine>
  /**
   * called when a world is created
   */
  val onWorldCreated: EventListeners1<World>

  /**
   * called when a world is destroyed
   */
  val onWorldDestroyed: EventListeners1<World>
}

class ConcurrentEngineEvents : EngineEvents {
  override val onEngineCreated = ImmediateEventListeners1<Engine>()
  override val onEngineDestroyed = ImmediateEventListeners1<Engine>()
  override val onWorldCreated = BufferedEventListeners1<World>()
  override val onWorldDestroyed = BufferedEventListeners1<World>()

  fun fire() {
    onWorldCreated.fire()
    onWorldDestroyed.fire()
  }

  fun clear() {
    onEngineCreated.listeners.clear()
    onEngineDestroyed.listeners.clear()
    onWorldCreated.buffer.clear()
    onWorldCreated.listeners.clear()
    onWorldDestroyed.buffer.clear()
    onWorldDestroyed.listeners.clear()
  }
}


// ==================================================================
//
// event objects used internally by DefaultEngine
//
// ==================================================================

data class EngineRequestWorldCreate(val worldName: String,
                                    val options: Any?,
                                    val builder: WorldBuilderLambda)

data class EngineRequestWorldDestroy(val worldName: String)

data class EngineRequestWorldUpdated(val worldName: String,
                                     val update: Boolean)

data class EngineRequestBlock(val block: Engine.() -> Unit)
