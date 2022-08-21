@file:Suppress("DuplicatedCode")

package fledware.ecs.benchmark

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import fledware.ecs.AbstractSystem
import fledware.ecs.Entity
import fledware.ecs.EntityGroup
import fledware.ecs.GroupIteratorSystem
import fledware.ecs.World
import fledware.ecs.WorldData
import fledware.ecs.componentIndexOf
import fledware.ecs.forEach
import fledware.ecs.util.MapperIndex

class FledCollisionSystem : AbstractSystem() {
  private lateinit var radiusIndex: MapperIndex<RadiusComponent>
  private lateinit var entities: EntityGroup

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    radiusIndex = data.componentIndexOf()
    entities = data.createEntityGroup { radiusIndex in it }
  }

  override fun update(delta: Float) { }
}

class FledMovementSystem : GroupIteratorSystem() {

  private val tmp = Vector2()
  private lateinit var positionIndex: MapperIndex<PositionComponent>
  private lateinit var movementIndex: MapperIndex<MovementComponent>

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    positionIndex = data.componentIndexOf()
    movementIndex = data.componentIndexOf()
  }

  override fun includeEntity(entity: Entity): Boolean {
    return positionIndex in entity && movementIndex in entity
  }

  override fun processEntity(entity: Entity, delta: Float) {
    val position = entity[positionIndex]
    val movement = entity[movementIndex]
    tmp.set(movement.accel).scl(delta)
    movement.velocity.add(tmp)
    tmp.set(movement.velocity).scl(delta)
    position.pos.add(tmp.x, tmp.y, 0.0f)
  }
}

class FledRemovalSystem : AbstractSystem() {
  private lateinit var removalIndex: MapperIndex<RemovalComponent>
  private lateinit var entities: EntityGroup

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    removalIndex = data.componentIndexOf()
    entities = data.createEntityGroup { removalIndex in it }
  }

  override fun update(delta: Float) {
    entities.forEach { data.removeEntity(it) }
  }
}

class FledStateSystem : GroupIteratorSystem() {
  private lateinit var stateIndex: MapperIndex<StateComponent>

  override fun onCreate(world: World, data: WorldData) {
    super.onCreate(world, data)
    stateIndex = data.componentIndexOf()
  }

  override fun includeEntity(entity: Entity): Boolean {
    return stateIndex in entity
  }

  override fun processEntity(entity: Entity, delta: Float) {
    entity[stateIndex].time += delta
  }
}

class FledRandomDeleter(worldAt: Int = -1) : AbstractSystem() {
  private var counter = 0
  private val counterReset: Int by lazy {
    Constants.FRAMES_PER_REMOVAL * world.engine.data.worlds.size
  }
  private val removeAt: Int by lazy {
    if (worldAt < 0)
      return@lazy Constants.FRAMES_PER_REMOVAL
    return@lazy Constants.FRAMES_PER_REMOVAL * (worldAt + 1)
  }

  override fun update(delta: Float) {
    counter++
    if (counter == removeAt)
      data.entities.values().random().add(RemovalComponent())
    if (counter > counterReset)
      counter = 0
  }
}

class FledRandomAdder(private val worldAt: Int = -1) : AbstractSystem() {
  private var counter = 0
  private val counterReset: Int by lazy {
    Constants.FRAMES_PER_REMOVAL * world.engine.data.worlds.size
  }
  private val removeAt: Int by lazy {
    if (worldAt < 0)
      return@lazy Constants.FRAMES_PER_REMOVAL
    return@lazy Constants.FRAMES_PER_REMOVAL * (worldAt + 1)
  }

  override fun update(delta: Float) {
    counter++
    if (counter == removeAt)
      data.createEntity { stdWorldEntity(counter) }
    if (counter > counterReset)
      counter = 0
  }
}



fun Entity.stdWorldEntity(entityIndex: Int) {
  if (Constants.shouldHaveComponent(ComponentType.POSITION, entityIndex)) {
    val pos = PositionComponent()
    pos.pos.x = MathUtils.random(Constants.MIN_POS, Constants.MAX_POS)
    pos.pos.y = MathUtils.random(Constants.MIN_POS, Constants.MAX_POS)
    add(pos)
  }
  if (Constants.shouldHaveComponent(ComponentType.MOVEMENT, entityIndex)) {
    val mov = MovementComponent()
    mov.velocity.x = MathUtils.random(Constants.MIN_VEL, Constants.MAX_VEL)
    mov.velocity.y = MathUtils.random(Constants.MIN_VEL, Constants.MAX_VEL)
    mov.accel.x = MathUtils.random(Constants.MIN_ACC, Constants.MAX_ACC)
    mov.accel.y = MathUtils.random(Constants.MIN_ACC, Constants.MAX_ACC)
    add(mov)
  }
  if (Constants.shouldHaveComponent(ComponentType.RADIUS, entityIndex)) {
    val rad = RadiusComponent()
    rad.radius = MathUtils.random(Constants.MIN_RADIUS, Constants.MAX_RADIUS)
    add(rad)
  }
  if (Constants.shouldHaveComponent(ComponentType.STATE, entityIndex)) {
    add(StateComponent())
  }
}
