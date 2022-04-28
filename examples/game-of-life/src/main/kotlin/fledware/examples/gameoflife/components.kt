package fledware.examples.gameoflife

import com.badlogic.gdx.math.Rectangle

/**
 * engine level information
 */
data class EngineInfo(var isEven: Boolean,
                      val worldCount: Int,
                      val worldSize: Int) {
  val maxCellLocation = worldCount * worldSize
}

/**
 * world level information.
 */
data class WorldInfo(val worldX: Int,
                     val worldY: Int,
                     val size: Int) {
  val bounds = Rectangle()
}

/**
 * the global location for the given entity
 */
data class CellLocation(val x: Int,
                        val y: Int)

/**
 * The component that holds alive data for the entity.
 *
 * We switch between updating even and odd alive. This
 * is so threads can read on value and write another without
 * having concurrency issues.
 */
data class CellAlive(var evenAlive: Boolean,
                     var oddAlive: Boolean) {
  fun isAlive(isEven: Boolean): Boolean {
    return if (isEven) evenAlive else oddAlive
  }

  fun setNextGen(isEven: Boolean, alive: Boolean) {
    if (isEven) oddAlive = alive
    else evenAlive = alive
  }

  fun setThisGen(isEven: Boolean, alive: Boolean) {
    if (isEven) evenAlive = alive
    else oddAlive = alive
  }
}

/**
 * We use this to cache the neighbors. This should be
 * set up before any updates.
 */
data class CellNeighbors(val neighbors: List<CellAlive>) {
  fun aliveCount(isEven: Boolean): Int = neighbors.sumOf {
    if (it.isAlive(isEven)) 1 else 0.toInt()
  }
}
