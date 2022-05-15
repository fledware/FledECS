package fledware.ecs

import fledware.utilities.MutableTypedMap

/**
 * We want to have a common interface for building a world
 * because it's heavily dependent on the EngineUpdateStrategy.
 */
interface WorldBuilder : EntityFactory {
  /**
   * The name of the world to be built.
   */
  var name: String
  /**
   * The ordered update group this world will belong to.
   *
   * @see EngineUpdateStrategy.createWorldUpdateGroup
   */
  var updateGroup: String
  /**
   * User defined options passed into [Engine.requestCreateWorld].
   */
  var options: Any?
  /**
   * Components to be added to [WorldData.components]
   */
  val components: MutableTypedMap<Any>
  /**
   * All systems created for the world so far.
   */
  val systems: List<System>
  /**
   * All entities created for this world so far.
   */
  val entities: List<Entity>
  /**
   * All the named entities.
   */
  val entitiesNamed: Map<String, Entity>
  /**
   * All the entity groups that will be created with the given filter.
   */
  val entityGroups: Map<String, (Entity) -> Boolean>

  /**
   * Adds a system.
   */
  fun addSystem(system: System)

  /**
   * Adds an [EntityGroup] to [WorldData.entityGroups] on creation of
   * the [World]. The entity group will have the given name and filter
   * based on [include].
   */
  fun entityGroup(name: String, include: (Entity) -> Boolean)

  /**
   * Builds the world. Generally called by the engine.
   */
  fun build(): WorldManaged
}

/**
 * A decorator that modifies the [WorldBuilder].
 */
typealias WorldBuilderDecorator = WorldBuilder.() -> Unit
