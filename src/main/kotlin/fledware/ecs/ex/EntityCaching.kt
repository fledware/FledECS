package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineData
import fledware.ecs.Entity
import fledware.ecs.EntityFactory
import fledware.ecs.WorldData
import fledware.ecs.entityComponentIndexOf
import fledware.ecs.getOrAdd
import fledware.ecs.getOrNull
import fledware.ecs.util.MapperIndex
import fledware.utilities.get
import fledware.utilities.getMaybe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max


// ==================================================================
//
//
//
// ==================================================================

fun Engine.withEntityCaching(defaultSize: Int = 100): Engine {
  data.components.put(EntityCaching(defaultSize, data.entityComponentIndexOf()))
  return this
}

class EntityCaching(val defaultSize: Int,
                    val cacheInfoIndex: MapperIndex<CacheInfo>) {
  private val buckets = ConcurrentHashMap<String, EntityCachingBucket>()
  fun getBucket(name: String) = buckets.computeIfAbsent(name) {
    EntityCachingBucket(defaultSize)
  }
}

class EntityCachingBucket(defaultSize: Int) {
  var maxCached = defaultSize
    set(value) {
      field = max(0, value)
      while (field < cached.size) {
        cached.remove()
      }
    }
  val hasSpace: Boolean
    get() = cached.size < maxCached
  private val cached = ConcurrentLinkedQueue<Entity>()

  fun offer(entity: Entity) {
    if (hasSpace)
      cached += entity
  }

  fun take(): Entity? {
    val result = cached.poll() ?: return null
    for (component in result.components.data)
      (component as? CachingComponent)?.reset()
    return result
  }
}

/**
 * The reset method will get called automatically before
 * WorldData.createCachedEntity returns the entity.
 */
interface CachingComponent {
  fun reset()
}

val EngineData.caching: EntityCaching
  get() = components.get()

val EngineData.cachingMaybe: EntityCaching?
  get() = components.getMaybe()


// ==================================================================
//
//
//
// ==================================================================

fun EntityFactory.createCachedEntity(bucket: String, onCreateDecorator: Entity.() -> Unit): Entity {
  val caching = engine.data.cachingMaybe
  val result = caching?.getBucket(bucket)?.take()
      ?: engine.data.createEntity(onCreateDecorator)
  if (caching != null && caching.cacheInfoIndex !in result)
    result.add(caching.cacheInfoIndex, CacheInfo(bucket))
  importEntity(result)
  return result
}

fun WorldData.removeEntityToCache(entity: Entity) {
  removeEntity(entity)
  val caching = engine.data.cachingMaybe ?: return
  val bucket = entity[caching.cacheInfoIndex].bucket
  if (bucket.isNotEmpty())
    caching.getBucket(bucket).offer(entity)
}

fun WorldData.clearEntitiesToCache() {
  val entities = clearEntities()
  val caching = engine.data.cachingMaybe ?: return
  entities.forEach {
    val bucket = it[caching.cacheInfoIndex].bucket
    if (bucket.isNotEmpty())
      caching.getBucket(bucket).offer(it)
  }
}


// ==================================================================
//
//
//
// ==================================================================

/**
 * the entity component used by this extension for info.
 * This will get automatically added when used.
 */
data class CacheInfo(var bucket: String)

/**
 * the cache bucket that this entity belongs to
 */
var Entity.cacheBucket: String
  get() = getOrNull<CacheInfo>()?.bucket ?: ""
  set(value) {
    getOrAdd { CacheInfo("") }.bucket = value
  }
