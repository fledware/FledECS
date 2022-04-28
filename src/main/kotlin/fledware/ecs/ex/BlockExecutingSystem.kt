package fledware.ecs.ex

import fledware.ecs.AbstractSystem
import fledware.ecs.World
import fledware.ecs.WorldManaged
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
fun World.execute(block: BlockExecutingSystem.() -> Unit) {
  (this as WorldManaged).dataSafe.systems[BlockExecutingSystem::class].execute(block)
}
