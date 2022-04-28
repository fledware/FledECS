package fledware.ecs.util

import java.util.concurrent.ConcurrentHashMap

// ==================================================================
//
// The three classes in this file work together to make objects in an array
// very quickly accessible. This is the main algorithm used for accessing
// data across an entire system without knowing what those concrete
// types are before-hand.
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
// is generally created by the [Mapper], and ensures the index is the
// same for each time the same key is handed in.
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
class MapperList<K: Any, V: Any>(val mapper: Mapper<K>) {
  // The data that holds all the objects. This is left public
  // so others can do crazy things if they feel like it.
  var data = Array<Any?>(mapper.size) { null }
    private set

  // Kind of a hack, but this will allow updates to propagate
  // automatically without more object allocation. Only one listener
  // at a time and intended only for use internally.
  var onUpdateObject: Any? = null
  var onUpdate: ((Any?) -> Unit)? = null
  fun fireOnUpdate() = onUpdate?.invoke(onUpdateObject)

  fun clear() {
    for (i in data.indices) {
      data[i] = null
    }
    fireOnUpdate()
  }

  operator fun get(index: Int): V = getOrNull(index)
          ?: throw IndexOutOfBoundsException("index not found: $index => ${mapper.reverseLookup(index)}")

  fun getOrNull(index: Int): V? = if (index in data.indices) data[index] as V? else null

  fun getOrDefault(index: Int, default: V): V = getOrNull(index) ?: default

  fun getOrSet(index: Int, default: V): V = getOrNull(index) ?: set(index, default)!!

  fun getOrCreate(index: Int, block: () -> V): V = getOrNull(index) ?: set(index, block())!!


  operator fun get(key: K): V = get(mapper[key])

  fun getOrNull(key: K): V? = getOrNull(mapper[key])

  fun getOrDefault(key: K, default: V): V = getOrDefault(mapper[key], default)

  fun getOrSet(key: K, default: V): V = getOrSet(mapper[key], default)

  fun getOrCreate(key: K, block: () -> V): V = getOrCreate(mapper[key], block)


  operator fun <T: V> get(index: MapperIndex<T>): T = get(index.index) as T

  fun <T: V> getOrNull(index: MapperIndex<T>): T? = getOrNull(index.index) as T?

  fun <T: V> getOrDefault(index: MapperIndex<T>, default: T): T = getOrDefault(index.index, default) as T

  fun <T: V> getOrSet(index: MapperIndex<T>, default: T): T = getOrSet(index.index, default) as T

  fun <T: V> getOrCreate(index: MapperIndex<T>, block: () -> T): T = getOrCreate(index.index, block) as T


  operator fun contains(index: Int): Boolean = getOrNull(index) != null

  operator fun contains(key: K): Boolean = getOrNull(key) != null

  operator fun contains(index: MapperIndex<*>): Boolean = getOrNull(index.index) != null


  operator fun set(index: Int, value: V?): V? {
    if (index !in data.indices) {
      data = data.copyOf(mapper.size)
    }
    data[index] = value
    fireOnUpdate()
    return value
  }

  fun setIfNull(index: Int, value: V): V {
    return getOrNull(index) ?: set(index, value)!!
  }

  fun setOrThrow(index: Int, value: V): V {
    if (index in this)
      throw IllegalStateException("already set: ${mapper.reverseLookup(index)} => $index")
    return set(index, value)!!
  }

  operator fun set(key: K, value: V?): V? = set(mapper[key], value)

  fun setIfNull(key: K, value: V): V = setIfNull(mapper[key], value)

  fun setOrThrow(key: K, value: V): V = setOrThrow(mapper[key], value)

  operator fun <T: V> set(index: MapperIndex<T>, value: T?): T? = set(index.index, value) as T?

  fun <T: V> setIfNull(index: MapperIndex<T>, value: T): T = setIfNull(index.index, value) as T

  fun <T: V> setOrThrow(index: MapperIndex<T>, value: T): T = setOrThrow(index.index, value) as T


  fun unset(index: Int): V? {
    if (index !in data.indices) {
      return null
    }
    val result = data[index] ?: return null
    fireOnUpdate()
    return result as V
  }

  fun unset(key: K): V? = unset(mapper[key])

  fun <T: V> unset(index: MapperIndex<T>): T? = unset(index.index) as T?
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
