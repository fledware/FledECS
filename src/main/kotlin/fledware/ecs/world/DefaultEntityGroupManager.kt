package fledware.ecs.world

import fledware.ecs.Entity
import fledware.ecs.EntityGroup
import fledware.ecs.internal.InternalEntityGroupManager
import fledware.ecs.World
import fledware.ecs.util.getRandomString
import java.util.concurrent.ConcurrentHashMap

open class DefaultEntityGroupManager(
    final override val world: World
) : InternalEntityGroupManager {
  private val groups = ConcurrentHashMap<String, DefaultEntityGroup>()

  override val size: Int
    get() = groups.size

  override fun get(name: String): EntityGroup {
    return groups[name] ?: throw IllegalStateException("group not found: $name")
  }

  override fun create(name: String, include: (entity: Entity) -> Boolean): EntityGroup {
    val groupName: String = name.ifEmpty {
      // just to make sure we don't accidentally create two of the same name...
      // very, very, unlikely... but would be extremely difficult to understand if
      // it did happen.
      while (true) {
        val check = getRandomString(15)
        if (check !in groups.keys)
          return@ifEmpty check
      }
      // when this isn't here, the compiler says this method returns Unit...
      // when this is here, it says this is unreachable code...
      // so, what do you want?
      @Suppress("UNREACHABLE_CODE")
      throw IllegalStateException("this is a bug... should not have got here")
    }
    val group = DefaultEntityGroup(world, groupName, include)
    if (groups.putIfAbsent(groupName, group) != null)
      throw IllegalStateException("group already exists: $groupName")
    return group
  }

  override fun remove(name: String) {
    val group = groups.remove(name) ?: return
    group.destroy()
  }

  override fun fireEvents() {
    groups.values.forEach { it.fireEvents() }
  }

  override fun clearEntities() {
    groups.values.forEach { it.clearEntities() }
  }

  override fun destroy() {
    groups.values.forEach { it.destroy() }
    groups.clear()
  }
}