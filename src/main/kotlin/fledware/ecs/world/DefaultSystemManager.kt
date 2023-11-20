package fledware.ecs.world

import fledware.ecs.internal.InternalSystemManager
import fledware.ecs.OnSystemAdded
import fledware.ecs.OnSystemOrderChange
import fledware.ecs.OnSystemRemoved
import fledware.ecs.System
import fledware.ecs.World
import fledware.ecs.util.CachedArray
import fledware.ecs.util.add
import fledware.utilities.RootDefaultTypedMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
open class DefaultSystemManager(protected val world: World) : InternalSystemManager {
  override val systems = RootDefaultTypedMap<System>()
  protected val systemsCache = CachedArray {
    systems.values.sortedBy { it.order }.toTypedArray()
  }
  protected var updating = false
  protected val operations = ConcurrentLinkedQueue<SystemOperation>()

  override val size: Int
    get() = systems.size

  protected val onSystemOrderChange = world.updateEvents.add<OnSystemOrderChange> {
    systemsCache.clear()
  }

  override fun add(system: System) {
    if (updating) {
      operations += SystemOperationAdd(system)
      return
    }
    systems.add(system)
    system.onCreate(world)
    systemsCache.clear()
    world.updateEvents.fire(OnSystemAdded(system))
  }

  override fun remove(key: KClass<out System>) {
    if (updating) {
      operations += SystemOperationRemove(key)
      return
    }
    val system = systems.remove(key) ?: return
    system.onDestroy()
    systemsCache.clear()
    world.updateEvents.fire(OnSystemRemoved(system))
  }

  override fun destroy() {
    if (updating) {
      operations += SystemOperationDestroy
      return
    }
    onSystemOrderChange.remove()
    systems.values.forEach { it.onDestroy() }
    systems.clear()
    systemsCache.clear()
  }

  override fun startSystemUpdateIteration(): Array<System> {
    updating = true
    return systemsCache()
  }

  override fun finishedSystemUpdateIteration() {
    updating = false
    handleOperations()
  }

  protected interface SystemOperation
  protected data class SystemOperationAdd(val system: System) : SystemOperation
  protected data class SystemOperationRemove(val key: KClass<out System>) : SystemOperation
  protected data object SystemOperationDestroy : SystemOperation

  protected open fun handleOperations() {
    while (true) {
      val operation = operations.poll() ?: break
      handleOperation(operation)
    }
  }

  protected open fun handleOperation(operation: SystemOperation) {
    when (operation) {
      is SystemOperationAdd -> add(operation.system)
      is SystemOperationRemove -> remove(operation.key)
      is SystemOperationDestroy -> destroy()
      else -> throw IllegalStateException("invalid operation: $operation")
    }
  }
}