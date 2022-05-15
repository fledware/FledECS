package fledware.ecs.impl

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.EntityEvents
import fledware.ecs.EntityGroup
import fledware.ecs.EntityGroupManaged
import fledware.ecs.ManagedEntity
import fledware.ecs.System
import fledware.ecs.WorldData
import fledware.ecs.WorldManaged
import fledware.ecs.sendEntity
import fledware.ecs.sendMessage
import fledware.ecs.util.getRandomString
import fledware.utilities.debug
import fledware.utilities.trace
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * This is a simple implementation of the world. It just
 * iterates over all the systems for updates. This assumes
 * the underline data in the world is locked to the caller
 * thread of update, and will not be accessed by other
 * threads while updating.
 *
 * This world has performance advantages because of how
 * update is handled. It will just iterate over all the systems
 * and run updates, while not synchronizing on anything. This will
 * make message passing and entity updates always safe and atomic
 * for the given data (within the given world).
 *
 * The major con of this has to do with exactly above. You can not
 * safely thread updates within [EntityGroup]s. So if you have
 * no reason to have multiple worlds, then you cannot gain performance
 * improvements with threading.
 */
class DefaultWorld(override val engine: Engine,
                   override val name: String,
                   override val updateGroup: String,
                   override val options: Any?)
  : WorldManaged {
  companion object {
    private val logger = LoggerFactory.getLogger(DefaultWorld::class.java)
  }

  private val entitiesToReceive = ConcurrentLinkedQueue<Entity>()

  private val messagesToReceive = ConcurrentLinkedQueue<Any>()

  override val events = DefaultWorldEvents()

  override var updateIndex: Long = 0
    private set

  init {
    if (engine.options.paranoidWorldEvents) {
      events.onEntityDeleted += this::ensureUnownedEntity
      events.onEntityLeft += this::ensureUnownedEntity
      events.onEntityRemoved += this::ensureUnownedEntity
      events.onEntityCreated += this::ensureMyEntity
      events.onEntityReceived += this::ensureMyEntity
      events.onEntityAdded += this::ensureMyEntity
      events.onEntityChanged += this::ensureMyEntity
    }
  }

  private val updating = AtomicBoolean(false)


  private fun assertNotUpdating() {
    if (updating.get())
      throw IllegalStateException("cannot call while updating")
  }


  // ================================================================
  //
  // data interface
  //
  // ================================================================

  private val _data = object : AbstractWorldData(engine, this) {

    override fun addSystem(system: System) {
      logger.debug { "adding a system to world: $name -> $system" }
      systemsMutable.add(system)
      systemsListCache = null
      systemsToCreate += system
    }

    override fun <S : System> removeSystem(key: KClass<S>) {
      logger.debug { "removing a system from world: $name -> $key" }
      val system = systemsMutable.remove(key)
          ?: throw IllegalStateException("system key not found: $key")
      systemsListCache = null
      systemsToRemove += system
    }

    override fun createEntityGroup(name: String, include: (entity: Entity) -> Boolean): EntityGroup {
      val groupName = name.ifEmpty { getRandomString(15) }
      if (entityGroupsMutable.containsKey(groupName))
        throw IllegalStateException("group already exists: $groupName")
      val result = engine.updateStrategy.entityGroup(include)
      result.attachWorld(this@DefaultWorld, this)
      entityGroupsMutable.put(groupName, result)
      return result
    }

    override fun removeEntityGroup(name: String) {
      val group = entityGroupsMutable.removeKey(name)
          ?: throw IllegalStateException("group not managed by this world: $name")
      group.finished()
    }

    override fun removeEntityGroup(group: EntityGroup) {
      val name = entityGroupsMutable.getKey(group as EntityGroupManaged)
          ?: throw IllegalStateException("group not managed by this world: $group")
      removeEntityGroup(name)
    }

    override fun clearEntities(): List<Entity> {
      assertNotUpdating()
      entitiesNamedMutable.clear()
      val result = entitiesMutable.values().toList()
      result.forEach { it.components.onUpdate = null }
      entitiesMutable.clear()
      entityGroupsMutable.forEachValue { it.clear() }
      events.clearAllEntitiesForRemove()
      return result
    }

    override fun removeEntity(entity: Entity) {
      entityDelete(entity)
    }

    override fun sendEntity(world: String, entity: Entity) {
      entityLeft(entity)
      engine.sendEntity(world, entity)
    }

    override fun sendMessage(world: String, message: Any) {
      engine.sendMessage(world, message)
    }

    override fun importEntity(entity: Entity) {
      entityCreate(entity)
    }

    override fun createEntity(decorator: Entity.() -> Unit): Entity {
      val entity = engine.data.createEntity(decorator)
      entityCreate(entity)
      return entity
    }

    override fun createEntity(name: String, decorator: Entity.() -> Unit): Entity {
      val entity = engine.data.createEntity {
        this.name = name
        this.decorator()
      }
      entityCreate(entity)
      return entity
    }

    override fun clearCaches() {
      this.systemsListCache = null
    }
  }

  override val data: WorldData
    get() {
      assertNotUpdating()
      return _data
    }

  override val dataSafe: WorldData
    get() = _data

  // ================================================================
  //
  // updates
  //
  // ================================================================

  override fun preUpdate() {
    updating.set(true)
    handleSystemUpdates()
    handlerExternalMessages()
  }

  override fun update(delta: Float) {
    updateIndex++
    logger.trace { "$name update $updateIndex" }
    events.fireAllEvents()
    for (system in _data.systemsList) {
      val shouldUpdate = system.enabled && system !in _data.systemsToRemove
      logger.trace { "   $name updating $system: $shouldUpdate" }
      if (shouldUpdate) {
        system.update(delta)
        events.fireEntityEvents()
      }
    }
  }

  override fun postUpdate() {
    updating.set(false)
  }

  override fun onDestroy() {
    _data.systemsMutable.values.forEach { it.onDestroy() }
    _data.systemsMutable.clear()
    events.clear()
    _data.clearEntities()
    _data.entityGroupsMutable.forEachValue { it.finished() }
    _data.entityGroupsMutable.clear()
  }

  override fun onCreate() {
    handleSystemUpdates()
    handlerExternalMessages()
    events.fireAllEvents()
  }

  private fun handleSystemUpdates() {
    while (true) {
      val system = _data.systemsToCreate.removeFirstOrNull() ?: break
      system.onCreate(this, _data)
      events.onSystemAdded(system)
    }
    while (true) {
      val system = _data.systemsToRemove.removeFirstOrNull() ?: break
      system.onDestroy()
      events.onSystemRemoved(system)
    }
  }


  // ================================================================
  //
  // external message handling
  //
  // ================================================================

  override fun receiveMessage(message: Any) {
    messagesToReceive.add(message)
  }

  override fun receiveEntity(entity: Entity) {
    entitiesToReceive.add(entity)
  }

  private fun handlerExternalMessages() {
    while (true) {
      val entity = messagesToReceive.poll() ?: break
      events.onMessage(entity)
    }
    while (true) {
      val entity = entitiesToReceive.poll() ?: break
      entityReceive(entity)
    }
  }


  // ================================================================
  //
  // high level entity events
  //
  // ================================================================

  private val entityEvents = object : EntityEvents {
    override fun onNameChange(entity: Entity) {
      indexEntityName(entity)
    }

    override fun onUpdate(entity: Entity) {
      events.onEntityChanged(entity)
    }
  }

  private fun entityCreate(entity: Entity) {
    addEntity(entity)
    events.onEntityCreated(entity)
    events.onEntityAdded(entity)
  }

  private fun entityReceive(entity: Entity) {
    addEntity(entity)
    events.onEntityReceived(entity)
    events.onEntityAdded(entity)
  }

  private fun entityDelete(entity: Entity) {
    events.onEntityDeleted(entity)
    events.onEntityRemoved(entity)
    removeEntity(entity)
    events.clearEntityForRemove(entity)
  }

  private fun entityLeft(entity: Entity) {
    events.onEntityLeft(entity)
    events.onEntityRemoved(entity)
    removeEntity(entity)
    events.clearEntityForRemove(entity)
  }


  // ================================================================
  //
  // entity event checks
  //
  // ================================================================

  private fun ensureMyEntity(entity: Entity) {
    if (entity.worldSafe != name)
      throw IllegalStateException("entity not owned by world: $name != ${entity.worldSafe}")
    if (!_data.entitiesMutable.containsKey(entity.id))
      throw IllegalStateException("entity not in world: $entity")
  }

  private fun ensureUnownedEntity(entity: Entity) {
    if (entity.worldSafe != null)
      throw IllegalStateException("entity is owned: ${entity.worldSafe}")
  }


  // ================================================================
  //
  // entity operations
  //
  // ================================================================

  private fun addEntity(entity: Entity) {
    if (_data.entitiesMutable.containsKey(entity.id))
      throw IllegalStateException("id already exists in world: ${entity.id}")
    (entity as ManagedEntity).registerToWorld(name, entityEvents)
    _data.entitiesMutable.put(entity.id, entity)
    indexEntityName(entity)
  }

  private fun removeEntity(entity: Entity) {
    if (!_data.entitiesMutable.containsKey(entity.id))
      throw IllegalStateException("entity not in world: $entity")
    (entity as ManagedEntity).unregisterWorld()
    _data.entitiesMutable.remove(entity.id)
    _data.entitiesNamedMutable.removeValue(entity)
  }

  private fun indexEntityName(entity: Entity) {
    _data.entitiesNamedMutable.removeValue(entity)
    if (entity.hasName) {
      if (_data.entitiesNamedMutable.containsKey(entity.name))
        throw IllegalStateException("named entity already exists: ${entity.name}")
      _data.entitiesNamedMutable.put(entity.name, entity)
    }
  }
}