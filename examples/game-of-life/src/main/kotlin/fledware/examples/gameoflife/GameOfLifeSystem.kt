package fledware.examples.gameoflife

import fledware.ecs.Entity
import fledware.ecs.GroupIteratorSystem
import fledware.ecs.componentIndexOf
import fledware.utilities.get

class GameOfLifeSystem : GroupIteratorSystem("cells") {
  private val engineInfo by lazy { engine.data.components.get<EngineInfo>() }
  private val cellAliveIndex by lazy { data.componentIndexOf<CellAlive>() }
  private val cellNeighborsIndex by lazy { data.componentIndexOf<CellNeighbors>() }

  override fun includeEntity(entity: Entity): Boolean {
    return cellAliveIndex in entity && cellNeighborsIndex in entity
  }

  override fun processEntity(entity: Entity, delta: Float) {
    val cellAlive = entity[cellAliveIndex]
    val cellNeighbors = entity[cellNeighborsIndex]
    val nextGenAlive = when (cellNeighbors.aliveCount(engineInfo.isEven)) {
      2 -> cellAlive.isAlive(engineInfo.isEven)
      3 -> true
      else -> false
    }
    cellAlive.setNextGen(engineInfo.isEven, nextGenAlive)
  }
}