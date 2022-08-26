package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineData
import fledware.ecs.Entity
import fledware.ecs.EntityFactory
import fledware.ecs.WorldData
import fledware.ecs.componentIndexOf
import fledware.ecs.util.MapperIndex
import fledware.utilities.get
import fledware.utilities.getOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max


// ==================================================================
//
//
//
// ==================================================================

/**
 * Adds entity caching to the engine.
 *
 * The api for caching is meant to be a pass-through if caching
 * isn't enabled.
 */
fun Engine.withEntityCaching(defaultSize: Int = 100): Engine {
  data.contexts.put(EntityCaching(defaultSize, data.componentIndexOf()))
  return this
}

/**
 * The context used in [EngineData.contexts] for entity caching.
 */
class EntityCaching(val defaultSize: Int,
                    val cacheInfoIndex: MapperIndex<CacheInfo>) {
  private val buckets = ConcurrentHashMap<String, EntityCachingBucket>()

  /**
   * gets or creates a bucket with [name].
   *
   * [EntityCachingBucket.maxCached] will be set to [defaultSize].
   */
  fun getBucket(name: String) = buckets.computeIfAbsent(name) {
    EntityCachingBucket(name, defaultSize)
  }
}

/**
 * The bucket for a single
 */
class EntityCachingBucket(val name: String, defaultSize: Int) {
  /**
   * gets or sets the max allowed entities.
   */
  var maxCached = defaultSize
    set(value) {
      field = max(0, value)
      while (field < cached.size) {
        cached.remove()
      }
    }
  /**
   * the current amount of entities in this bucket
   */
  val size: Int
    get() = cached.size
  /**
   *
   */
  val hasSpace: Boolean
    get() = cached.size < maxCached

  private val cached = ConcurrentLinkedQueue<Entity>()

  /**
   * Attempt to put the entity in this bucket. It will not save
   * the entity if [hasSpace] returns false.
   */
  fun offer(entity: Entity) {
    if (hasSpace)
      cached += entity
  }

  /**
   * Attempts to get an entity out of the cache. If an entity
   * exists then [CachingComponent.reset] will be called on every
   * component that extends [CachingComponent].
   */
  fun take(): Entity? {
    val result = cached.poll() ?: return null
    for (component in result.data.data)
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

/**
 * Gets [EntityCaching]
 *
 * @throws IllegalStateException if entity caching is disabled
 */
val EngineData.caching: EntityCaching
  get() = contexts.get()

/**
 * Gets [EntityCaching] or returns null if entity caching is disabled.
 */
val EngineData.cachingOrNull: EntityCaching?
  get() = contexts.getOrNull()


// ==================================================================
//
//
//
// ==================================================================

/**
 * Tries to reuse an entity from the given [bucket]. This method will call
 * [EntityFactory.importEntity] automatically. An entity will be created if:
 * - entity caching is not enabled.
 * - there are no entities in the cache bucket.
 *
 * If an entity is pulled from the cache, [CachingComponent.reset] will be
 * called on every component that implements [CachingComponent].
 *
 * Using this method is safe if caching is disabled. It's suggested to use
 * these methods if an extension or part of the system could use caching.
 */
fun EntityFactory.createCachedEntity(bucket: String, onCreateDecorator: Entity.() -> Unit): Entity {
  val caching = engine.data.cachingOrNull
  val result = caching?.getBucket(bucket)?.take()
      ?: engine.data.createEntity(onCreateDecorator)
  if (caching != null && caching.cacheInfoIndex !in result)
    result.add(caching.cacheInfoIndex, CacheInfo(bucket))
  importEntity(result)
  return result
}

/**
 * Removes the entity by calling [WorldData.removeEntity].
 *
 * After, if caching is enabled, the entity will be put into the
 * [Entity.cacheBucket] bucket. If the bucket name is empty, then
 * the entity will not be saved.
 *
 * Using this method is safe if caching is disabled. It's suggested to use
 * these methods if an extension or part of the system could use caching.
 */
fun WorldData.removeEntityToCache(entity: Entity) {
  removeEntity(entity)
  val caching = engine.data.cachingOrNull ?: return
  val bucket = entity.getOrNull(caching.cacheInfoIndex)?.bucket ?: ""
  if (bucket.isNotEmpty())
    caching.getBucket(bucket).offer(entity)
}

/**
 * Removes all entities by calling [WorldData.clearEntities].
 *
 * All the entities will be put into their buckets specified
 * by [Entity.cacheBucket]. If the bucket is empty, then the entity
 * will not be saved.
 *
 * Using this method is safe if caching is disabled. It's suggested to use
 * these methods if an extension or part of the system could use caching.
 */
fun WorldData.clearEntitiesToCache() {
  val entities = clearEntities()
  val caching = engine.data.cachingOrNull ?: return
  entities.forEach {
    val bucket = it.getOrNull(caching.cacheInfoIndex)?.bucket ?: ""
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
 * the static index for entity caching info
 */
val cachingInfoIndex by StaticComponentMapperIndex<CacheInfo>()

/**
 * gets [EntityFlags] on the given entity or returns null
 */
val Entity.cachingInfoOrNull: CacheInfo?
  get() {
    val index = cachingInfoIndex ?: data.mapper.indexOf(CacheInfo::class)
    return getOrNull(index)
  }

/**
 * gets [EntityFlags] or adds it, then returns the instance.
 */
val Entity.cachingInfoOrAdd: CacheInfo
  get() {
    val index = cachingInfoIndex ?: data.mapper.indexOf(CacheInfo::class)
    return getOrAdd(index) { CacheInfo() }
  }

/**
 * the entity component used by this extension for info.
 * This will get automatically added when used.
 */
data class CacheInfo(var bucket: String = "")

/**
 * the cache bucket that this entity belongs to
 */
var Entity.cacheBucket: String
  get() = cachingInfoOrNull?.bucket ?: ""
  set(value) {
    cachingInfoOrAdd.bucket = value
  }
