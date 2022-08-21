package fledware.ecs.ex

import fledware.ecs.AbstractSystem
import fledware.ecs.World
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldData
import fledware.ecs.WorldManaged
import fledware.ecs.getOrNull
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * a system that can be added to execute code during
 * the next update call.
 */
class BlockExecutingSystem : AbstractSystem() {
  private val queue = ConcurrentLinkedQueue<BlockExecutingSystem.() -> Unit>()

  fun execute(block: BlockExecutingSystem.() -> Unit) {
    queue += block
  }

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    executeAll()
  }

  override fun update(delta: Float) {
    executeAll()
  }

  private fun executeAll() {
    while (true) {
      val block = queue.poll() ?: break
      this.block()
    }
  }
}

/**
 * Convenience method for adding [BlockExecutingSystem]
 */
fun WorldBuilder.withBlockExecutingSystem() {
  addSystem(BlockExecutingSystem())
}

/**
 * If the [BlockExecutingSystem] is in the world, then this method
 * can be called to execute the block on the next update call.
 *
 * Note, that the block may be called on the given update if this
 * method is called after the work update started, but before the internal
 * queue in the [BlockExecutingSystem] is empty.
 *
 * @param block the block to execute.
 */
fun World.execute(block: BlockExecutingSystem.() -> Unit) {
  val executing = (this as WorldManaged).dataSafe.systems.getOrNull<BlockExecutingSystem>()
      ?: throw IllegalStateException(
          "BlockExecutingSystem required to call execute." +
              " Call WorldBuilder.withBlockExecutingSystem during the world creation process")
  executing.execute(block)
}

/**
 * Convenience method for adding a one execution time with the given block.
 *
 * This requires [BlockExecutingSystem] and will error if the system is not
 * in the builder.
 */
fun WorldBuilder.initWith(block: BlockExecutingSystem.() -> Unit) {
  val executing = systems.find { it is BlockExecutingSystem }
      ?: throw IllegalStateException(
          "BlockExecutingSystem required to call initWith." +
              " Call WorldBuilder.withBlockExecutingSystem before this method.")
  (executing as BlockExecutingSystem).execute(block)
}
