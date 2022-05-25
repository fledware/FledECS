package fledware.ecs.ex

import fledware.ecs.World
import fledware.ecs.WorldData
import fledware.ecs.System
import fledware.ecs.WorldBuilder

/**
 * A system that can be used to initialize data on a world
 * during the [onCreate] cycles.
 *
 * The order is respected during the [onCreate] calls from
 * the world. The default for order is -100, which should
 * make it run early (if not first) during the create calls.
 */
class InitSystem(
    override val order: Int = -100,
    val block: (world: World, data: WorldData) -> Unit
) : System {
  override val enabled: Boolean = true

  override fun onCreate(world: World, data: WorldData) {
    block(world, data)
    data.removeSystem(this::class)
  }

  override fun update(delta: Float) {
    throw IllegalStateException("update should never be called on this system")
  }
}

/**
 * Convenience method for adding the [InitSystem] system
 * with the given block. The system will be removed after
 * the create process on the world.
 */
fun WorldBuilder.initWith(block: (world: World, data: WorldData) -> Unit) {
  addSystem(InitSystem(block = block))
}
