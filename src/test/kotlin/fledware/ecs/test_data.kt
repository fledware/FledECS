package fledware.ecs

import fledware.ecs.ex.BlockExecutingSystem
import fledware.ecs.ex.CachingComponent
import fledware.ecs.ex.createCachedEntity
import fledware.ecs.ex.sceneName
import fledware.ecs.impl.DefaultEngine

data class Placement(var x: Int, var y: Int, var size: Int)

data class Movement(var deltaX: Int, var deltaY: Int) : CachingComponent {
  override fun reset() {
    deltaX = 0
    deltaY = 0
  }
}

data class Health(var health: Int)

data class MapDimensions(var sizeX: Int, var sizeY: Int)

val WorldData.map get() = entitiesNamed["map"] ?: throw IllegalStateException("map not found")

class MovementSystem : UpdateCountSystem() {
  private val placementIndex by lazy { data.componentIndexOf<Placement>() }
  private val movementIndex by lazy { data.componentIndexOf<Movement>() }
  private val mapIndex by lazy { data.componentIndexOf<MapDimensions>() }
  val entities by lazy { data.createEntityGroup { placementIndex in it && movementIndex in it } }

  override fun update(delta: Float) {
    super.update(delta)
    entities.forEach { entity ->
      val map = data.map[mapIndex]
      val placement = entity[placementIndex]
      val movement = entity[movementIndex]

      placement.x += movement.deltaX
      placement.y += movement.deltaY
      movement.deltaX = 0
      movement.deltaY = 0

      if (placement.x < 0) placement.x = 0
      if (placement.x >= map.sizeX) placement.x = map.sizeX - 1
      if (placement.y < 0) placement.y = 0
      if (placement.y >= map.sizeY) placement.y = map.sizeY - 1
    }
  }
}

class UpdateCountSystemNotMovement : UpdateCountSystem()

class SomeSystemA(enabled: Boolean = true, order: Int = 0)
  : UpdateCountSystem(enabled, order)

class SomeSystemB(enabled: Boolean = true, order: Int = 0)
  : UpdateCountSystem(enabled, order)

class SomeSystemC(enabled: Boolean = true, order: Int = 0)
  : UpdateCountSystem(enabled, order)

class SomeSystemD(enabled: Boolean = true, order: Int = 0)
  : UpdateCountSystem(enabled, order)

open class UpdateCountSystem(enabled: Boolean = true,
                             order: Int = 0)
  : AbstractSystem(enabled, order) {
  var onDestroyCount = 0
  var onCreateCount = 0
  var onEnabledCount = 0
  var onDisabledCount = 0
  var updateCount = 0

  override fun onDestroy() {
    super.onDestroy()
    onDestroyCount++
  }

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    onCreateCount++
  }

  override fun onEnabled() {
    super.onEnabled()
    onEnabledCount++
  }

  override fun onDisabled() {
    super.onDisabled()
    onDisabledCount++
  }

  override fun update(delta: Float) {
    updateCount++
  }
}

fun EntityFactory.createPersonEntity(x: Int, y: Int): Entity {
  val result = createCachedEntity("person") {
    add(Placement(0, 0, 1))
    add(Movement(0, 0))
  }
  result.get<Placement>().x = x
  result.get<Placement>().y = y
  return result
}

fun WorldBuilder.worldBuilderMovementOnly() {
  addSystem(MovementSystem())
  addSystem(BlockExecutingSystem())
  createEntity {
    name = "map"
    add(MapDimensions(10, 10))
  }
  createPersonEntity(1, 1).name = "target"
  createPersonEntity(8, 8)
}

fun WorldBuilder.worldBuilderEmptyScene() {
  addSystem(MovementSystem())
  addSystem(BlockExecutingSystem())
  sceneName = ""
}

fun Engine.createTestWorld(name: String = "test"): World {
  return createWorldAndFlush(name, null, WorldBuilder::worldBuilderMovementOnly)
}

fun Engine.createEmptySceneWorld(name: String = "test"): World {
  return createWorldAndFlush(name, null, WorldBuilder::worldBuilderEmptyScene)
}

fun createTestEngine() = DefaultEngine().also { it.start() }
