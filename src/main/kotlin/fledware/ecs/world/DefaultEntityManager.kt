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
    final override val world: World
) : InternalEntityManager {
  override val entities = mutableMapOf<String, Entity>()

  // ================================================================
  // access
  // ================================================================

  override val size: Int
    get() = entities.size

  override fun getById(id: String): Entity {
    return entities[id]
        ?: throw IllegalStateException("entity not found with id $id")
  }

  override fun forEach(block: (entity: Entity) -> Unit) {
    entities.values.forEach { block(it) }
  }

  // ================================================================
  // addition
  // ================================================================

  override fun add(entity: Entity) {
    additionFinalize(entity, isCreate = false)
  }

  override fun create(decorator: Entity.() -> Unit): Entity {
    val result = world.hooks.onCreateEntity.list.firstNotNullOf { it(world) }
    result.decorator()
    additionFinalize(result, isCreate = true)
    return result
  }

  protected open fun additionFinalize(entity: Entity, isCreate: Boolean) {
    if (entities.containsKey(entity.id))
      throw IllegalStateException("entity with id already exists: $entity")
    entities[entity.id] = entity
    (entity as ManagedEntity).registerToWorld(world)
    world.updateEvents.fire(OnEntityAdded(entity, isCreate))
  }

  // ================================================================
  // removal
  // ================================================================

  override fun delete(entity: Entity) {
    entities.remove(entity.id)
    removalFinalize(entity, isDelete = true)
  }

  override fun deleteById(id: String) {
    val entity = entities.remove(id) ?: return
    removalFinalize(entity, isDelete = true)
  }

  override fun remove(entity: Entity) {
    entities.remove(entity.id)
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
    val result = entities.values.toList()
    entities.clear()
    world.updateEvents.fire(OnEntitiesCleared(result, isDelete))
    return result
  }

  // ================================================================
  // destroy
  // ================================================================

  override fun destroy() {
    entities.clear()
  }
}
