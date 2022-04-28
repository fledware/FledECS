package fledware.ecs.benchmark

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class AshleyCollisionSystem : EntitySystem() {
  var entities: ImmutableArray<Entity>? = null
  override fun addedToEngine(engine: Engine) {
    entities = engine.getEntitiesFor(Family.all(RadiusComponent::class.java).get())
  }

  override fun update(deltaTime: Float) {}
}

class AshleyMovementSystem : IteratingSystem(Family.all(PositionComponent::class.java, MovementComponent::class.java).get()) {
  private val tmp = Vector2()
  private val positionMapper = ComponentMapper.getFor(PositionComponent::class.java)
  private val movementMapper = ComponentMapper.getFor(MovementComponent::class.java)
  public override fun processEntity(entity: Entity, deltaTime: Float) {
    val position = positionMapper[entity]
    val movement = movementMapper[entity]

    tmp.set(movement.accel).scl(deltaTime)
    movement.velocity.add(tmp)
    tmp.set(movement.velocity).scl(deltaTime)
    position.pos.add(tmp.x, tmp.y, 0.0f)
  }
}

class AshleyRemovalSystem : EntitySystem() {
  private lateinit var entities: ImmutableArray<Entity>
  override fun addedToEngine(engine: Engine) {
    entities = engine.getEntitiesFor(Family.all(RemovalComponent::class.java).get())
  }

  override fun update(deltaTime: Float) {
    entities.forEach { engine.removeEntity(it) }
  }
}

class AshleyStateSystem : IteratingSystem(Family.all(StateComponent::class.java).get()) {
  private val stateMapper = ComponentMapper.getFor(StateComponent::class.java)
  public override fun processEntity(entity: Entity, deltaTime: Float) {
    stateMapper[entity].time += deltaTime
  }
}

class AshleyRandomAdder : EntitySystem() {
  private var counter = 0
  override fun update(deltaTime: Float) {
    counter++
    if (counter % Constants.FRAMES_PER_REMOVAL == 0)
      engine.addEntity(Entity().apply { stdWorldEntity(counter) })
  }
}

class AshleyRandomDeleter : EntitySystem() {
  private var counter = 0
  override fun update(deltaTime: Float) {
    counter++
    if (counter % Constants.FRAMES_PER_REMOVAL == 0)
      engine.entities.random().add(RemovalComponent())
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
