package fledware.ecs.impl

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.System
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldManaged
import fledware.utilities.DefaultTypedMap
import fledware.utilities.MutableTypedMap

/**
 * The standard datastructures that will likely be needed
 * for building a world.
 */
abstract class AbstractWorldBuilder(final override val engine: Engine,
                                    override var name: String,
                                    override var options: Any?)
  : WorldBuilder {
  override var updateGroup: String = engine.options.defaultUpdateGroupName
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
    // the world is created, we don't want to reuse this builder.
    if (builderFinished)
      throw IllegalStateException("builder already used")
    builderFinished = true
    return actualBuild()
  }

  protected abstract fun actualBuild(): WorldManaged
}
