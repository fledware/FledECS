package fledware.ecs

interface EntityManager {
  /**
   * the amount of managed entities
   */
  val size: Int
  /**
   *
   */
  val entitiesById: Map<String, Entity>
  /**
   *
   */
  val entitiesByName: Map<String, Entity>
  /**
   * the world this manager is for
   */
  val world: World
  /**
   * gets an [Entity] by id
   */
  fun getById(id: String): Entity
  /**
   * gets an [Entity] by name
   */
  fun getByName(name: String): Entity
  /**
   *
   */
  fun forEach(block: (entity: Entity) -> Unit)
  /**
   * create an entity and automatically add it to this manager.
   *
   * this will cause an [OnEntityCreated] event.
   */
  fun create(name: String = "", decorator: Entity.() -> Unit): Entity
  /**
   * this is called when an entity is created externally from the world.
   *
   * @see World.receiveEntity
   * @see
   */
  fun add(entity: Entity)
  /**
   * delete the given entity from the world.
   *
   * this will cause an [OnEntityDeleted] event.
   */
  fun delete(entity: Entity)
  /**
   * delete the entity with the given name.
   *
   * this will cause an [OnEntityDeleted] event.
   */
  fun deleteByName(name: String)
  /**
   * delete the entity with the given id.
   *
   * this will cause an [OnEntityDeleted] event.
   */
  fun deleteById(id: String)
}
