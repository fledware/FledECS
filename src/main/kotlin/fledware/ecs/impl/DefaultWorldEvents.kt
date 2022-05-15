package fledware.ecs.impl

import fledware.ecs.Entity
import fledware.ecs.System
import fledware.ecs.WorldEvents
import fledware.ecs.util.BufferedEventListeners1


/**
 * default implementation that is thread safe
 */
class DefaultWorldEvents : WorldEvents {

  override val onMessage = BufferedEventListeners1<Any>()
  override val onSystemAdded = BufferedEventListeners1<System>()
  override val onSystemRemoved = BufferedEventListeners1<System>()
  override val onEntityDeleted = BufferedEventListeners1<Entity>()
  override val onEntityLeft = BufferedEventListeners1<Entity>()
  override val onEntityRemoved = BufferedEventListeners1<Entity>()
  override val onEntityCreated = BufferedEventListeners1<Entity>()
  override val onEntityReceived = BufferedEventListeners1<Entity>()
  override val onEntityAdded = BufferedEventListeners1<Entity>()
  override val onEntityChanged = BufferedEventListeners1<Entity>()

  fun fireAllEvents() {
    onMessage.fire()
    onSystemAdded.fire()
    onSystemRemoved.fire()
    onEntityDeleted.fire()
    onEntityLeft.fire()
    onEntityRemoved.fire()
    onEntityCreated.fire()
    onEntityReceived.fire()
    onEntityAdded.fire()
    onEntityChanged.fire()
  }

  fun fireEntityEvents() {
    onEntityDeleted.fire()
    onEntityLeft.fire()
    onEntityRemoved.fire()
    onEntityCreated.fire()
    onEntityReceived.fire()
    onEntityAdded.fire()
    onEntityChanged.fire()
  }

  fun clearEntityForRemove(entity: Entity) {
    onEntityCreated.removeEvent(entity)
    onEntityReceived.removeEvent(entity)
    onEntityAdded.removeEvent(entity)
    onEntityChanged.removeEvent(entity)
  }

  fun clearAllEntitiesForRemove() {
    onEntityDeleted.buffer.clear()
    onEntityLeft.buffer.clear()
    onEntityRemoved.buffer.clear()
    onEntityCreated.buffer.clear()
    onEntityReceived.buffer.clear()
    onEntityAdded.buffer.clear()
    onEntityChanged.buffer.clear()
  }

  fun clear() {
    onMessage.buffer.clear()
    onMessage.listeners.clear()
    onSystemAdded.buffer.clear()
    onSystemAdded.listeners.clear()
    onSystemRemoved.buffer.clear()
    onSystemRemoved.listeners.clear()
    onEntityDeleted.buffer.clear()
    onEntityDeleted.listeners.clear()
    onEntityLeft.buffer.clear()
    onEntityLeft.listeners.clear()
    onEntityRemoved.buffer.clear()
    onEntityRemoved.listeners.clear()
    onEntityCreated.buffer.clear()
    onEntityCreated.listeners.clear()
    onEntityReceived.buffer.clear()
    onEntityReceived.listeners.clear()
    onEntityAdded.buffer.clear()
    onEntityAdded.listeners.clear()
    onEntityChanged.buffer.clear()
    onEntityChanged.listeners.clear()
  }
}
