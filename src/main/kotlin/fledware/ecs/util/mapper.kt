package fledware.ecs.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// ==================================================================
//
// The three classes in this file work together to make objects in an array
// very quickly accessible. This is the main algorithm used for accessing
// data across an entire system without knowing what those concrete
// types are beforehand.
//
// The classes here are generally hidden behind even more convenience
// methods, but are left open because they have turned out to be helpful
// in other use cases as well. The two main uses are:
// - Entity components
// - Entity flags
//
// The [Mapper] class will keep track of all indexes that already exist
// and give ways to lookup and "map" indexes to a type, and types to an index.
//
// The [MapperIndex] is a convenience class that can be used to
// quickly access actual data on an array of unknown types. A [MapperIndex]
// is created by the [Mapper], and ensures the index is the same for each
// time the same key is handed in.
//
// The [MapperList] holds the actual data and has many ways of accessing
// the list. Using a [MapperIndex] will have the fastest way to access
// data because it is only a single array lookup.
//
// ==================================================================


/**
 * keeps an index for the values to ensure consistent random access
 * with arrays that index based on the values.
 */
class Mapper<T: Any> {
  private val _objects = mutableListOf<T>()

  private val _objectsToIds = ConcurrentHashMap<T, Int>()

  val size: Int get() = _objects.size

  operator fun get(obj: T): Int = _objectsToIds.computeIfAbsent(obj) {
    _objects.add(obj)
    _objects.size - 1
  }

  operator fun contains(obj: T) = _objectsToIds.containsKey(obj)

  operator fun contains(id: Int) = id in _objects.indices

  fun reverseLookup(id: Int): T = _objects[id]

  fun <V: Any> list() = MapperList<T, V>(this)

  fun <V: Any> concurrentList() = ConcurrentMapperList<T, V>(this)

  fun <V: Any> indexOf(key: T) = MapperIndex<V>(this[key])
}


/**
 * Used by [Mapper] to give fast access and automatically
 * cast the returned object to the given type.
 */
data class MapperIndex<T: Any>(val index: Int)


/**
 * Keeps an index for the values to ensure consistent random access
 * with arrays that index based on the values.
 */
@Suppress("UNCHECKED_CAST")
open class MapperList<K: Any, V: Any>(val mapper: Mapper<K>) {
  // The data that holds all the objects. This is left public
  // so others can do crazy things if they feel like it.
  var data = Array<Any?>(mapper.size) { null }
    private set

  // Kind of a hack, but this will allow updates to propagate
  // automatically without more object allocation. Only one listener
  // at a time and intended only for use internally.
  var onUpdate: (() -> Unit)? = null
  fun fireOnUpdate() = onUpdate?.invoke()

  fun clear() {
    for (i in data.indices) {
      data[i] = null
    }
    fireOnUpdate()
  }

  protected open fun grow() {
    data = data.copyOf(mapper.size)
  }

  fun getByIndex(index: Int): V = getByIndexOrNull(index)
          ?: throw IndexOutOfBoundsException("index not found: $index => ${mapper.reverseLookup(index)}")

  fun getByIndexOrNull(index: Int): V? =
      if (index in data.indices) data[index] as V? else null

  fun getByIndexOrDefault(index: Int, default: V): V =
      getByIndexOrNull(index) ?: default

  fun getByIndexOrSet(index: Int, default: V): V =
      getByIndexOrNull(index) ?: setByIndex(index, default)!!

  fun getByIndexOrCreate(index: Int, block: () -> V): V =
      getByIndexOrNull(index) ?: setByIndex(index, block())!!


  fun <T: V> getByIndex(index: MapperIndex<T>): T =
      getByIndex(index.index) as T

  fun <T: V> getByIndexOrNull(index: MapperIndex<T>): T? =
      getByIndexOrNull(index.index) as T?

  fun <T: V> getByIndexOrDefault(index: MapperIndex<T>, default: T): T =
      getByIndexOrDefault(index.index, default) as T

  fun <T: V> getByIndexOrSet(index: MapperIndex<T>, default: T): T =
      getByIndexOrSet(index.index, default) as T

  fun <T: V> getByIndexOrCreate(index: MapperIndex<T>, block: () -> T): T =
      getByIndexOrCreate(index.index, block) as T


  fun getByKey(key: K): V =
      getByIndex(mapper[key])

  fun getByKeyOrNull(key: K): V? =
      getByIndexOrNull(mapper[key])

  fun getByKeyOrDefault(key: K, default: V): V =
      getByIndexOrDefault(mapper[key], default)

  fun getByKeyOrSet(key: K, default: V): V =
      getByIndexOrSet(mapper[key], default)

  fun getByKeyOrCreate(key: K, block: () -> V): V =
      getByIndexOrCreate(mapper[key], block)


  fun containsIndex(index: Int): Boolean =
      getByIndexOrNull(index) != null

  fun containsKey(key: K): Boolean =
      getByKeyOrNull(key) != null

  fun containsIndex(index: MapperIndex<*>): Boolean =
      getByIndexOrNull(index.index) != null


  fun setByIndex(index: Int, value: V?): V? {
    if (index !in data.indices)
      grow()
    data[index] = value
    fireOnUpdate()
    return value
  }

  fun setByIndexIfNull(index: Int, value: V): V {
    return getByIndexOrNull(index) ?: setByIndex(index, value)!!
  }

  fun setByIndexOrThrow(index: Int, value: V): V {
    if (this.containsIndex(index))
      throw IllegalStateException("already set: ${mapper.reverseLookup(index)} => $index")
    return setByIndex(index, value)!!
  }


  fun <T: V> setByIndex(index: MapperIndex<T>, value: T?): T? =
      setByIndex(index.index, value) as T?

  fun <T: V> setByIndexIfNull(index: MapperIndex<T>, value: T): T =
      setByIndexIfNull(index.index, value) as T

  fun <T: V> setByIndexOrThrow(index: MapperIndex<T>, value: T): T =
      setByIndexOrThrow(index.index, value) as T


  fun setByKey(key: K, value: V?): V? =
      setByIndex(mapper[key], value)

  fun setByKeyIfNull(key: K, value: V): V =
      setByIndexIfNull(mapper[key], value)

  fun setByKeyOrThrow(key: K, value: V): V =
      setByIndexOrThrow(mapper[key], value)


  fun unsetByIndex(index: Int): V? {
    if (index !in data.indices) {
      return null
    }
    val result = data[index] ?: return null
    fireOnUpdate()
    return result as V
  }

  fun unsetByKey(key: K): V? = unsetByIndex(mapper[key])

  fun <T: V> unsetByIndex(index: MapperIndex<T>): T? = unsetByIndex(index.index) as T?
}

inline fun <K: Any, V: Any> MapperList<K, V>.forEach(block: (key: K, value: V?) -> Unit) {
  this.data.forEachIndexed { index, value ->
    val key = mapper.reverseLookup(index)
    @Suppress("UNCHECKED_CAST")
    block(key, value as V?)
  }
}

inline fun <K: Any, V: Any> MapperList<K, V>.forEachNotNull(block: (key: K, value: V) -> Unit) {
  this.data.forEachIndexed { index, value ->
    val key = mapper.reverseLookup(index)
    val notNullValue = value ?: return@forEachIndexed
    @Suppress("UNCHECKED_CAST")
    block(key, notNullValue as V)
  }
}

class ConcurrentMapperList<K: Any, V: Any>(mapper: Mapper<K>) : MapperList<K, V>(mapper) {
  private val growLock = ReentrantLock()

  override fun grow() = growLock.withLock { super.grow() }
}
