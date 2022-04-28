package fledware.ecs

import fledware.utilities.DefaultTypedMap
import fledware.utilities.MutableTypedMap

/**
 * We want to have a common interface for building a world
 * because it's heavily dependent on the EngineUpdateStrategy.
 */
interface WorldBuilder : EntityFactory {
  var name: String
  var options: Any?
  val components: MutableTypedMap<Any>
  val systems: List<System>
  val entities: List<Entity>
  val entitiesNamed: Map<String, Entity>
  val entityGroups: Map<String, (Entity) -> Boolean>

  fun addSystem(system: System)
  fun entityGroup(name: String, include: (Entity) -> Boolean)
  fun build(): WorldManaged
}

typealias WorldBuilderLambda = WorldBuilder.() -> Unit

abstract class AbstractWorldBuilder(override val engine: Engine,
                                    override var name: String,
                                    override var options: Any?)
  : WorldBuilder {
  override val systems = mutableListOf<System>()
  override val entities = mutableListOf<Entity>()
  override val entitiesNamed = mutableMapOf<String, Entity>()
  override val entityGroups = mutableMapOf<String, (Entity) -> Boolean>()
  override val components: MutableTypedMap<Any> = DefaultTypedMap()
  var builderFinished = false
    private set

  override fun addSystem(system: System) {
    systems += system
  }

  override fun entityGroup(name: String, include: (Entity) -> Boolean) {
    if (entityGroups.put(name, include) != null)
      throw IllegalStateException("group already exists: $name")
  }

  override fun importEntity(entity: Entity) {
    entities.add(entity)
    if (entity.hasName)
      entitiesNamed[entity.name] = entity
  }

  override fun createEntity(decorator: Entity.() -> Unit): Entity {
    val result = engine.data.createEntity(decorator)
    entities.add(result)
    if (result.hasName)
      entitiesNamed[result.name] = result
    return result
  }

  final override fun build(): WorldManaged {
    // we should not be reusing entity or system instances, so once
    // the world is created, we dont want to reuse this builder.
    if (builderFinished)
      throw IllegalStateException("builder already used")
    builderFinished = true
    return actualBuild()
  }

  protected abstract fun actualBuild(): WorldManaged
}