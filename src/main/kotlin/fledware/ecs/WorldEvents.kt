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
