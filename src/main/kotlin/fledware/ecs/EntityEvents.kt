package fledware.ecs

/**
 * when a component is added or removed.
 *
 * Also, if [Entity.notifyUpdate] is called.
 */
data class OnEntityUpdated(
    val entity: Entity
)

/**
 * when an entity is added to an [EntityManager]. [isCreate] is
 * true when the entity is created via [EntityManager.create].
 *
 * [isCreate] is false if the entity is created externally and
 * added via [World.receiveEntity]. This will generally happen
 * if entities are being transferred between worlds or loading/unloading
 * of groups of entities.
 */
data class OnEntityAdded(
    val entity: Entity,
    val isCreate: Boolean
)

/**
 * when an entity is removed from an [EntityManager]. [isDelete] is
 * true when the entity is removed via [Entity.delete] or [EntityManager.delete].
 *
 * [isDelete] is false if the entity is removed from the system
 * in other ways, generally transferring to other [World]s
 */
data class OnEntityRemoved(
    val entity: Entity,
    val isDelete: Boolean
)

/**
 *
 */
data class OnEntitiesCleared(
    val entities: List<Entity>,
    val isDelete: Boolean
)
