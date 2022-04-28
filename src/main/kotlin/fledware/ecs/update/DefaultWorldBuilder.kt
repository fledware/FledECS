package fledware.ecs.update

import fledware.ecs.AbstractWorldBuilder
import fledware.ecs.Engine
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldManaged

/**
 * implementation of a [WorldBuilder] that can be used
 * to build a [DefaultWorld] instance.
 */
class DefaultWorldBuilder(engine: Engine,
                          name: String,
                          options: Any?)
  : AbstractWorldBuilder(engine, name, options) {

  override fun actualBuild(): WorldManaged {
    val result = DefaultWorld(engine, name, options)
    components.values.forEach { result.data.components.put(it) }
    systems.forEach { result.data.addSystem(it) }
    entities.forEach { result.receiveEntity(it) }
    entityGroups.forEach { result.data.createEntityGroup(it.key, it.value) }
    return result
  }
}