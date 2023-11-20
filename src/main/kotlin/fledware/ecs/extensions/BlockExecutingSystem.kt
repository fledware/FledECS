package fledware.ecs.extensions

import fledware.ecs.AbstractSystem
import fledware.ecs.World
import fledware.ecs.getOrNull
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * setup for usage of [World.executeInUpdate]
 */
fun World.withBlockExecutingSystem(order: Int = 0): World {
  this.systems.add(BlockExecutingSystem(order))
  return this
}

/**
 * a system that can be added to execute code during
 * the next update call.
 */
class BlockExecutingSystem(order: Int) : AbstractSystem(order = order) {
  private val queue = ConcurrentLinkedQueue<BlockExecutingSystem.() -> Unit>()

  fun execute(block: BlockExecutingSystem.() -> Unit) {
    queue += block
  }

  override fun update(delta: Float) {
    while (true) {
      val block = queue.poll() ?: break
      this.block()
    }
  }
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
fun World.executeInUpdate(block: BlockExecutingSystem.() -> Unit) {
  val executing = this.systems.systems.getOrNull<BlockExecutingSystem>()
      ?: throw IllegalStateException(
          "BlockExecutingSystem required to call execute." +
              " Call World.withBlockExecutingSystem during the world creation process")
  executing.execute(block)
}
