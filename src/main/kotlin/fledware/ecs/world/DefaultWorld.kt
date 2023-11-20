@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName")

package fledware.ecs.world

import fledware.ecs.Entity
import fledware.ecs.EntityGroupManager
import fledware.ecs.EntityManager
import fledware.ecs.System
import fledware.ecs.SystemManager
import fledware.ecs.World
import fledware.ecs.WorldHooks
import fledware.ecs.internal.DefaultEntityFactory
import fledware.ecs.internal.EntityFactory
import fledware.ecs.internal.systemUpdateIteration
import fledware.ecs.util.DefaultBufferedEventBus
import fledware.utilities.DefaultTypedMap
import fledware.utilities.MutableTypedMap
import fledware.utilities.trace
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("LeakingThis")
open class DefaultWorld(
    final override val name: String = "main",
    entityFactory: EntityFactory = DefaultEntityFactory()
) : World {
  private val logger = LoggerFactory.getLogger("${this::class.qualifiedName}.$name")

  override val contexts: MutableTypedMap<Any> = DefaultTypedMap()

  override val hooks: WorldHooks = DefaultWorldHooks()
  init {
    hooks.onCreateEntity += entityFactory
  }

  override val updateEvents = DefaultBufferedEventBus()

  protected open val _entities = DefaultEntityManager(this)
  override val entities: EntityManager
    get() = _entities

  protected open val _entityGroups = DefaultEntityGroupManager(this)
  override val entityGroups: EntityGroupManager
    get() = _entityGroups

  protected open val _systems = DefaultSystemManager(this)
  override val systems: SystemManager
    get() = _systems

  override var updateIndex: Long = 0
    protected set

  protected val received = ConcurrentLinkedQueue<Any>()

  override fun receiveMessage(message: Any) {
    received.add(message)
  }

  override fun receiveEntity(entity: Entity) {
    received.add(entity)
  }

  override fun deleteEntities() {
    _entities.deleteEntities()
    _entityGroups.clearEntities()
  }

  override fun removeEntities(): List<Entity> {
    val result = _entities.removeEntities()
    _entityGroups.clearEntities()
    return result
  }

  override fun update(delta: Float) {
    updateIndex++
    hooks.onBeforeUpdate.forEach { it(this) }
    handleReceivedValues()
    updateEvents.fireBufferedEvents()
    _entityGroups.fireEvents()
    _systems.systemUpdateIteration { system: System ->
      updateSystem(system, delta)
    }
    hooks.onAfterUpdate.forEach { it(this) }
  }

  protected open fun updateSystem(system: System, delta: Float) {
    val shouldUpdate = system.enabled
    logger.trace { "   $name updating $system: $shouldUpdate" }
    if (shouldUpdate) {
      system.update(delta)
      updateEvents.fireBufferedEvents()
      _entityGroups.fireEvents()
    }
  }

  protected open fun handleReceivedValues() {
    while (true) {
      val check = received.poll() ?: break
      when (check) {
        is Entity -> _entities.add(check)
        else -> updateEvents.fire(check)
      }
    }
  }

  override fun destroy() {
    hooks.onWorldDestroy.forEach { it(this) }
    contexts.clear()
    _entities.destroy()
    _entityGroups.destroy()
    _systems.destroy()
    updateEvents.clearListeners()
    updateEvents.clearBufferedEvents()
  }
}