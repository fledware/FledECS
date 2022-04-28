package fledware.ecs.update

import fledware.ecs.Entity
import fledware.ecs.EntityGroupManaged
import fledware.ecs.World
import fledware.ecs.WorldData
import fledware.ecs.util.ImmediateEventListeners0
import fledware.ecs.util.UniqueList

open class DefaultEntityGroup(private val include: (entity: Entity) -> Boolean)
  : EntityGroupManaged {

  private var _world: World? = null
  override val world: World
    get() = _world ?: throw IllegalStateException("no world attached")

  private val entityUniqueList = UniqueList<Entity>()
  override val entities get() = entityUniqueList.list
  override val onChange = ImmediateEventListeners0()

  override fun contains(entity: Entity): Boolean = entity in entityUniqueList

  override fun iterator(): Iterator<Entity> = entities.iterator()

  override fun attachWorld(world: World, data: WorldData) {
    if (_world != null)
      throw IllegalStateException("world already attached")
    _world = world
    world.events.onEntityAdded += this::onEntityAdded
    world.events.onEntityRemoved += this::onEntityRemoved
    world.events.onEntityChanged += this::onEntityChanged
    data.entities.values().forEach { onEntityAdded(it) }
  }

  override fun clear() {
    entityUniqueList.clear()
  }

  override fun finished() {
    if (_world == null)
      return
    entityUniqueList.clear()
    world.events.onEntityAdded -= this::onEntityAdded
    world.events.onEntityRemoved -= this::onEntityRemoved
    world.events.onEntityChanged -= this::onEntityChanged
    _world = null
  }

  private fun onEntityAdded(entity: Entity) {
    if (include(entity) && entityUniqueList.add(entity))
      onChange()
  }

  private fun onEntityRemoved(entity: Entity) {
    if (entityUniqueList.remove(entity))
      onChange()
  }

  private fun onEntityChanged(entity: Entity) {
    if (include(entity)) {
      if (entityUniqueList.add(entity))
        onChange()
    }
    else {
      if (entityUniqueList.remove(entity))
        onChange()
    }
  }
}
