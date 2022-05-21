package fledware.examples.gameoflife

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.EntityFactory
import fledware.ecs.World
import fledware.ecs.componentIndexOf
import fledware.ecs.get
import fledware.utilities.get
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Finds an entity based on the location of the entity.
 *
 * This can only be called outside the engine update cycle because
 * it reads protected data from the target world.
 */
fun Engine.findEntityByLocation(x: Int, y: Int, worldSize: Int): Entity? {
  if (x < 0 || y < 0) return null
  val entityName = "$x,$y"
  val worldName = "${x / worldSize},${y / worldSize}"
  val world = data.worlds[worldName] ?: return null
  return world.data.entitiesNamed[entityName] ?: throw IllegalArgumentException(
      "entity not found for [$x, $y] in world $worldName: $entityName")
}

/**
 * A helper that creates a standard empty cell
 */
fun EntityFactory.createCell(x: Int, y: Int) = createEntity {
  name = "$x,$y"
  add(CellLocation(x, y))
  add(CellAlive(false, false))
}

/**
 * A helper that creates a standard empty world.
 */
fun Engine.createWorld(x: Int, y: Int, worldSize: Int) {
  requestCreateWorld("$x,$y") {
    components.put(WorldInfo(x, y, worldSize))
    addSystem(GameOfLifeSystem())
  }
}

/**
 * seeds the engine with all the cells and
 */
fun Engine.seedCells(worldsCount: Int, worldSize: Int) {
  data.contexts.add(EngineInfo(false, worldsCount, worldSize))

  // create all the worlds
  repeat(worldsCount) { worldY ->
    repeat(worldsCount) { worldX ->
      createWorld(worldX, worldY, worldSize)
    }
  }
  handleRequests()
  println("created ${data.worlds.size} worlds!")

  // fill all worlds with cells
  runBlocking {
    data.worlds.values.forEach { world ->
      launch {
        world.fillCells()
      }
    }
  }

  // now that all the entities are created and won't change
  // location, create the neighbor lists.
  runBlocking {
    data.worlds.values.forEach { world ->
      launch {
        world.fillCellNeighbors()
      }
    }
  }

  val totalEntities = data.worlds.values.sumOf { it.data.entities.size() }
  println("created a total of $totalEntities cells!")
}

/**
 * Creates all the empty cells for the given world.
 */
fun World.fillCells() {
  val info = data.contexts.get<WorldInfo>()
  repeat(info.size) { entityY ->
    repeat(info.size) { entityX ->
      data.createCell(
          info.worldX * info.size + entityX,
          info.worldY * info.size + entityY
      )
    }
  }
  println("created ${data.entities.size()} entities in world $name!")
}

/**
 * finds the [CellAlive] components of the 8 entities around
 * each entity. If the cell is on the edge, then those neighbors
 * are just not added.
 */
fun World.fillCellNeighbors() {
  val info = data.contexts.get<WorldInfo>()
  val aliveIndex = data.componentIndexOf<CellAlive>()
  data.entities.values().forEach { entity ->
    val (x, y) = entity.get<CellLocation>()
    val neighbors = mutableListOf<CellAlive>()
    engine.findEntityByLocation(x - 1, y - 1, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x + 0, y - 1, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x + 1, y - 1, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x - 1, y + 0, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x + 1, y + 0, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x - 1, y + 1, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x + 0, y + 1, info.size)?.also { neighbors += it[aliveIndex] }
    engine.findEntityByLocation(x + 1, y + 1, info.size)?.also { neighbors += it[aliveIndex] }
    entity.add(CellNeighbors(neighbors))
  }
}
