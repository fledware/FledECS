package fledware.ecs

import fledware.ecs.util.BufferedEventListeners1
import fledware.ecs.util.EventListeners1

/**
 * the events that can happen in a world
 */
interface WorldEvents {
  val onMessage: EventListeners1<Any>
  val onSystemAdded: EventListeners1<System>
  val onSystemRemoved: EventListeners1<System>
  val onEntityDeleted: EventListeners1<Entity>
  val onEntityLeft: EventListeners1<Entity>
  val onEntityRemoved: EventListeners1<Entity>
  val onEntityCreated: EventListeners1<Entity>
  val onEntityReceived: EventListeners1<Entity>
  val onEntityAdded: EventListeners1<Entity>
  val onEntityChanged: EventListeners1<Entity>
}

/**
 * default implementation that is thread safe
 */
class ConcurrentWorldEvents : WorldEvents {

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

  fun fireBufferedEvents() {
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

  fun clearBuffer() {
    onMessage.buffer.clear()
    onSystemAdded.buffer.clear()
    onSystemRemoved.buffer.clear()
    onEntityDeleted.buffer.clear()
    onEntityLeft.buffer.clear()
    onEntityRemoved.buffer.clear()
    onEntityCreated.buffer.clear()
    onEntityReceived.buffer.clear()
    onEntityAdded.buffer.clear()
    onEntityChanged.buffer.clear()
  }

  fun clearListeners() {
    onMessage.listeners.clear()
    onSystemAdded.listeners.clear()
    onSystemRemoved.listeners.clear()
    onEntityDeleted.listeners.clear()
    onEntityLeft.listeners.clear()
    onEntityRemoved.listeners.clear()
    onEntityCreated.listeners.clear()
    onEntityReceived.listeners.clear()
    onEntityAdded.listeners.clear()
    onEntityChanged.listeners.clear()
  }
}
