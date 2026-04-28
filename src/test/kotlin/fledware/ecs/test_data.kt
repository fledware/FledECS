package fledware.ecs

import fledware.ecs.extensions.CachingComponent
import fledware.ecs.extensions.getByName
import fledware.ecs.extensions.name
import fledware.ecs.extensions.withBlockExecutingSystem
import fledware.ecs.extensions.withEntityName

data class Placement(var x: Int, var y: Int, var size: Int)

data class Movement(var deltaX: Int, var deltaY: Int) : CachingComponent {
  override fun reset() {
    deltaX = 0
    deltaY = 0
  }
}

data class Health(var health: Int)

data class MapDimensions(var sizeX: Int, var sizeY: Int)

val EntityManager.map : Entity
  get() = this.getByName("map")

val EntityGroupManager.persons
  get() = this["persons"]

class ImmediateRemoveSystem : UpdateCountSystem() {
  override fun update(delta: Float) {
    super.update(delta)
    world.systems.remove(this::class)
  }
}

class MovementSystem : UpdateCountSystem() {
  private val placementIndex by lazy { world.componentIndexOf<Placement>() }
  private val movementIndex by lazy { world.componentIndexOf<Movement>() }
  private val mapIndex by lazy { world.componentIndexOf<MapDimensions>() }
  val entities by lazy { world.entityGroups.create("persons") { it.isPersonEntity() } }

  override fun onCreate(world: World) {
    super.onCreate(world)
    entities
  }

  override fun update(delta: Float) {
    super.update(delta)
    entities.forEach { entity ->
      val map = world.entities.map[mapIndex]
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

  override fun onCreate(world: World) {
    super.onCreate(world)
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

fun EntityManager.createPersonEntity(x: Int, y: Int): Entity = create {
  add(Placement(x, y, 1))
  add(Movement(0, 0))
}

fun EntityManager.createMapEntity(dimensionX: Int, dimensionY: Int) = create {
  this.name = "map"
  add(MapDimensions(dimensionX, dimensionY))
}

fun Entity.isPersonEntity(): Boolean {
  val placement = this.getOrNull<Placement>() ?: return false
  if (placement.size != 1) return false
  return this.contains<Movement>()
}

fun World.worldBuilderMovementOnly(): World {
  this.withEntityName()
  this.withBlockExecutingSystem()
  this.systems.add(MovementSystem())
  this.entities.createMapEntity(10, 10)
  this.entities.createPersonEntity(1, 1).name = "target"
  this.entities.createPersonEntity(8, 8)
  this.update(1f)
  return this
}

//fun World.worldBuilderMovementOnlyIndexed(index: Int) {
//  addSystem(MovementSystem())
//  addSystem(BlockExecutingSystem())
//  createEntity {
//    name = "map"
//    add(MapDimensions(50, 50))
//  }
//  createPersonEntity(index, 0).name = "target"
//}
//
//fun WorldBuilder.worldBuilderEmptyScene() {
//  addSystem(MovementSystem())
//  addSystem(BlockExecutingSystem())
//  sceneName = ""
//}
//
//fun Engine.createTestWorld(name: String = "test"): World {
//  return createWorldAndFlush(name, null, WorldBuilder::worldBuilderMovementOnly)
//}
//
//fun Engine.createEmptySceneWorld(name: String = "test"): World {
//  return createWorldAndFlush(name, null, WorldBuilder::worldBuilderEmptyScene)
//}
//
//fun createTestEngine() = DefaultEngine().also { it.start() }
