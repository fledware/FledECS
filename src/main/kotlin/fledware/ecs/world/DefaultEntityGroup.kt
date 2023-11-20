package fledware.ecs.world

import fledware.ecs.Entity
import fledware.ecs.OnEntityAdded
import fledware.ecs.OnEntityRemoved
import fledware.ecs.OnEntityUpdated
import fledware.ecs.World
import fledware.ecs.internal.InternalEntityGroup
import fledware.ecs.util.BufferedEventListeners0
import fledware.ecs.util.UniqueList
import fledware.ecs.util.add

class DefaultEntityGroup(
    override val world: World,
    override val name: String,
    val include: (entity: Entity) -> Boolean
) : InternalEntityGroup {

  private val entityUniqueList = UniqueList<Entity>()

  override val entities: List<Entity>
    get() = entityUniqueList.list

  override val size: Int
    get() = super.size

  override val onChange = BufferedEventListeners0()

  private val entityUpdatedSubscription = world.updateEvents.add<OnEntityUpdated> {
    val entity = it.entity
    if (include(entity)) {
      if (entityUniqueList.add(entity))
        onChange()
    }
    else {
      if (entityUniqueList.remove(entity))
        onChange()
    }
  }

  private val entityAddedSubscription = world.updateEvents.add<OnEntityAdded> {
    val entity = it.entity
    if (include(entity) && entityUniqueList.add(entity))
      onChange()
  }

  private val entityRemovedSubscription = world.updateEvents.add<OnEntityRemoved> {
    val entity = it.entity
    if (entityUniqueList.remove(entity))
      onChange()
  }

  init {
    world.entities.forEach {
      if (include(it))
        entityUniqueList.add(it)
    }
  }

  override fun contains(entity: Entity): Boolean = entity in entityUniqueList

  override fun iterator(): Iterator<Entity> = entityUniqueList.list.iterator()

  override fun clearEntities() {
    entityUniqueList.clear()
    onChange()
  }

  override fun fireEvents() {
    onChange.fire()
  }

  override fun destroy() {
    entityUpdatedSubscription.remove()
    entityAddedSubscription.remove()
    entityRemovedSubscription.remove()
    entityUniqueList.clear()
  }
}