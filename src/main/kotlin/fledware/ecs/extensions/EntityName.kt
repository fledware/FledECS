package fledware.ecs.extensions

import fledware.ecs.Entity
import fledware.ecs.EntityManager
import fledware.ecs.OnEntitiesCleared
import fledware.ecs.OnEntityAdded
import fledware.ecs.OnEntityRemoved
import fledware.ecs.ReactiveSystem
import fledware.ecs.World
import fledware.ecs.getOrAdd
import fledware.ecs.getOrNull
import fledware.ecs.util.Subscription
import fledware.ecs.util.add
import fledware.utilities.getOrNull


// ==================================================================
//
// setup
//
// ==================================================================

/**
 * Used to set up EntityFlag usage within the given [World].
 *
 * This method is idempotent, and will not replace the current version
 */
fun World.withEntityName(): World {
  this.systems.add(EntityNameExtension())
  return this
}

// ==================================================================
//
// world/system level
//
// ==================================================================

class EntityNameExtension : ReactiveSystem() {
  private lateinit var data: EntityNameData
  private lateinit var subOnEntityNameChanged: Subscription
  private lateinit var subOnEntityAdded: Subscription
  private lateinit var subOnEntityRemoved: Subscription
  private lateinit var subOnEntitiesCleared: Subscription

  override fun onCreate(world: World) {
    super.onCreate(world)

    data = EntityNameData()
    world.contexts.add(data)
    subOnEntityNameChanged = world.updateEvents.add<OnEntityNameChanged> { (entity, previousName) ->
      data.entitiesByName.remove(previousName)
      putEntityUniquely(entity)
    }
    subOnEntityAdded = world.updateEvents.add<OnEntityAdded> { (entity, _) ->
      putEntityUniquely(entity)
    }
    subOnEntityRemoved = world.updateEvents.add<OnEntityRemoved> { (entity, _) ->
      data.entitiesByName.remove(entity.name)
    }
    subOnEntitiesCleared = world.updateEvents.add<OnEntitiesCleared> {
      data.entitiesByName.clear()
    }
  }

  private fun putEntityUniquely(entity: Entity) {
    if (!entity.hasName)
      return
    val oldEntity = data.entitiesByName.putIfAbsent(entity.name, entity) ?: return
    if (oldEntity !== entity)
      throw IllegalStateException("entity with name ${entity.name} already exists")
  }
}

class EntityNameData {
  val entitiesByName = mutableMapOf<String, Entity>()
}

/**
 * when the name of an entity has changed
 */
data class OnEntityNameChanged(
    val entity: Entity,
    val previousName: String
)

fun World.getEntityByName(name: String): Entity {
  val check = contexts.getOrNull<EntityNameData>()
      ?: throw IllegalStateException(
          "EntityNameExtension not installed. Call World.withEntityName() before using named entities")
  return check.entitiesByName[name] ?: throw IllegalStateException("entity not found: $name")
}

fun EntityManager.getByName(name: String): Entity {
  return this.world.getEntityByName(name)
}

// ==================================================================
//
// entity level
//
// ==================================================================

/**
 * Component used on the Entity for flags.
 */
class EntityName : CachingComponent {
  var name: String = ""

  override fun reset() {
    name = ""
  }
}

/**
 * The name that this entity has. An entity name must
 * be unique for the world it belongs to. It doesn't need
 * to be unique to the engine.
 *
 * When an entity is passed to another world, it will not automatically
 * remove its own name. An error will be thrown if the receiving world
 * already has an entity with that name.
 */
var Entity.name: String
  get() {
    return entityNameOrNull?.name ?: ""
  }
  set(value) {
    val entityName = entityNameOrAdd
    val previous = entityName.name
    entityName.name = value
    worldSafe?.updateEvents?.fire(OnEntityNameChanged(this, previous))
    notifyUpdate()
  }

/**
 * The world that this entity belongs to.
 *
 * @throws IllegalStateException when this entity doesn't belong to a world
 */
val Entity.hasName: Boolean
  get() = this.name != ""

val Entity.entityNameOrNull: EntityName?
  get() = getOrNull()

val Entity.entityNameOrAdd: EntityName
  get() = getOrAdd { EntityName() }
