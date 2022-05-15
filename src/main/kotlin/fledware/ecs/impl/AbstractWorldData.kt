package fledware.ecs.impl

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.EntityGroup
import fledware.ecs.EntityGroupManaged
import fledware.ecs.System
import fledware.ecs.World
import fledware.ecs.WorldData
import fledware.ecs.util.BiDirectionalMap
import fledware.utilities.DefaultTypedMap
import fledware.utilities.RootDefaultTypedMap
import fledware.utilities.TypedMap
import org.eclipse.collections.api.map.primitive.LongObjectMap
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap


abstract class AbstractWorldData(override val engine: Engine,
                                 override val world: World)
  : WorldData {

  override val components = DefaultTypedMap()

  val entityGroupsMutable = BiDirectionalMap<String, EntityGroupManaged>()
  override val entityGroups: Map<String, EntityGroup> get() = entityGroupsMutable.keyToValue

  val entitiesMutable = LongObjectHashMap<Entity>()
  override val entities: LongObjectMap<Entity> get() = entitiesMutable

  val entitiesNamedMutable = BiDirectionalMap<String, Entity>()
  override val entitiesNamed: Map<String, Entity> get() = entitiesNamedMutable.keyToValue

  var systemsListCache: List<System>? = null
  val systemsList: List<System>
    get() {
      if (systemsListCache == null)
        systemsListCache = systemsMutable.values.toList()
      return systemsListCache!!
    }

  val systemsToCreate = ArrayDeque<System>()
  val systemsToRemove = ArrayDeque<System>()

  val systemsMutable = RootDefaultTypedMap<System>()
  override val systems: TypedMap<System> get() = systemsMutable
}
