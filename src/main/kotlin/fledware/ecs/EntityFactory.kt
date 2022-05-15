package fledware.ecs

/**
 * A common interface for things that create entities.
 *
 * If you have factory methods or want to create consistent ways to
 * building an [Entity], extension methods can be added to this interface.
 *
 * The two main implementors of this interface are [WorldData]
 * and [WorldBuilder].
 */
interface EntityFactory {
  /**
   * The engine this factory belongs to. Helpful for extensions.
   */
  val engine: Engine

  /**
   * Adds the given entity into the factory.
   */
  fun importEntity(entity: Entity)

  /**
   * Adds all the given entities into the factory.
   */
  fun importEntities(entities: List<Entity>) = entities.forEach { importEntity(it) }

  /**
   * Creates an entity and automatically imports it into the factory.
   */
  fun createEntity(decorator: Entity.() -> Unit): Entity

  /**
   * Creates a named entity and automatically imports it into the factory.
   */
  fun createEntity(name: String, decorator: Entity.() -> Unit) = createEntity {
    this.name = name
    this.decorator()
  }
}
