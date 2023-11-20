package fledware.ecs.world

import fledware.ecs.Entity
import fledware.ecs.internal.InternalEntityManager
import fledware.ecs.OnEntitiesCleared
import fledware.ecs.OnEntityAdded
import fledware.ecs.OnEntityRemoved
import fledware.ecs.World
import fledware.ecs.internal.ManagedEntity

@Suppress("MemberVisibilityCanBePrivate")
open class DefaultEntityManager(
    protected val world: World
) : InternalEntityManager {
  override val entitiesById = mutableMapOf<String, Entity>()
  override val entitiesByName = mutableMapOf<String, Entity>()

  // ================================================================
  // access
  // ================================================================

  override val size: Int
    get() = entitiesById.size

  override fun getById(id: String): Entity {
    return entitiesById[id]
        ?: throw IllegalStateException("entity not found with id $id")
  }

  override fun getByName(name: String): Entity {
    return entitiesByName[name]
        ?: throw IllegalStateException("entity not found with name $name")
  }

  override fun forEach(block: (entity: Entity) -> Unit) {
    entitiesById.values.forEach { block(it) }
  }

  // ================================================================
  // addition
  // ================================================================

  override fun add(entity: Entity) {
    additionFinalize(entity, isCreate = false)
  }

  override fun create(name: String, decorator: Entity.() -> Unit): Entity {
    val result = world.hooks.onCreateEntity.list.firstNotNullOf { it(world) }
    result.name = name
    result.decorator()
    additionFinalize(result, isCreate = true)
    return result
  }

  protected open fun additionFinalize(entity: Entity, isCreate: Boolean) {
    if (entitiesById.containsKey(entity.id))
      throw IllegalStateException("entity with id already exists: $entity")
    if (entity.hasName && entitiesByName.putIfAbsent(entity.name, entity) != null)
      throw IllegalStateException("entity with name already exists: $entity")
    entitiesById[entity.id] = entity

    (entity as ManagedEntity)
    entity.registerToWorld(world)
    world.updateEvents.fire(OnEntityAdded(entity, isCreate))
  }

  // ================================================================
  // removal
  // ================================================================

  override fun delete(entity: Entity) {
    entitiesById.remove(entity.id)
    entitiesByName.remove(entity.name)
    removalFinalize(entity, isDelete = true)
  }

  override fun deleteByName(name: String) {
    val entity = entitiesByName.remove(name) ?: return
    entitiesById.remove(entity.id)
    removalFinalize(entity, isDelete = true)
  }

  override fun deleteById(id: String) {
    val entity = entitiesById.remove(id) ?: return
    if (entity.hasName)
      entitiesByName.remove(entity.name)
    removalFinalize(entity, isDelete = true)
  }

  override fun remove(entity: Entity) {
    entitiesById.remove(entity.id)
    entitiesByName.remove(entity.name)
    removalFinalize(entity, isDelete = false)
  }

  protected open fun removalFinalize(entity: Entity, isDelete: Boolean) {
    (entity as ManagedEntity)
    if (entity.unregisterWorld())
      world.updateEvents.fire(OnEntityRemoved(entity, isDelete))
    if (isDelete)
      world.hooks.onDeleteEntity.forEach { it(world, entity) }
  }

  // ================================================================
  // clearing
  // ================================================================

  override fun removeEntities(): List<Entity> {
    return clearingFinalize(isDelete = false)
  }

  override fun deleteEntities() {
    clearingFinalize(isDelete = true)
  }

  protected open fun clearingFinalize(isDelete: Boolean): List<Entity> {
    val result = entitiesById.values.toList()
    entitiesById.clear()
    entitiesByName.clear()
    world.updateEvents.fire(OnEntitiesCleared(result, isDelete))
    return result
  }

  // ================================================================
  // destroy
  // ================================================================

  override fun destroy() {
    entitiesById.clear()
    entitiesByName.clear()
  }
}
