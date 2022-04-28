package fledware.examples.gameoflife

import driver.libgdxRun

fun main(args: Array<String>) {
  if (args.isNotEmpty() && args.size != 2)
    throw IllegalArgumentException("must have 0 or 2 args: [worldsCount] [worldSize]")

  // This is how many worlds there are in a direction.
  // The total world count will always be the square of that.
  val worldsCount = if (args.size == 2) args[0].toInt() else 5
  // Now, how many entities in one direction.
  val worldSize = if (args.size == 2) args[1].toInt() else 100

  libgdxRun {
    engine.seedCells(worldsCount, worldSize)
    GameOfLifeScreen()
  }
}
