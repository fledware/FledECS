package fledware.ecs.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
inline fun exec(block: () -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  block()
}

private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
fun getRandomString(length: Int): String {
  return (1..length)
      .map { allowedChars.random() }
      .joinToString("")
}


// ==================================================================
//
//
//
// ==================================================================

class CachedArray<T : Any>(val builder: () -> Array<T>) {
  private var cache: Array<T>? = null

  operator fun invoke(): Array<T> {
    if (cache == null) {
      cache = builder()
    }
    return cache!!
  }

  fun clear() {
    cache = null
  }
}


// ==================================================================
//
//
//
// ==================================================================

interface OrderedValue {
  val order: Int
}

class OrderedMap<K : Any, V : OrderedValue>(
    val map: MutableMap<K, V> = mutableMapOf()
) {
  var orderedValuesCache: List<V>? = null

  val size: Int get() = map.size

  fun isEmpty(): Boolean = map.isEmpty()

  fun containsKey(key: K): Boolean = map.containsKey(key)

  fun containsValue(value: @UnsafeVariance V): Boolean = map.containsValue(value)

  operator fun get(key: K): V? = map[key]

  inline fun getOrPut(key: K, defaultValue: () -> V) = map.getOrPut(key) {
    orderedValuesCache = null
    defaultValue()
  }

  fun putIfAbsent(key: K, value: V): V? {
    orderedValuesCache = null
    return map.putIfAbsent(key, value)
  }

  fun put(key: K, value: V): V? {
    orderedValuesCache = null
    return map.put(key, value)
  }

  fun remove(key: K): V? {
    orderedValuesCache = null
    return map.remove(key)
  }

  fun clear() {
    orderedValuesCache = null
    map.clear()
  }

  fun orderedValues(): List<V> {
    val orderedValuesCacheCheck = orderedValuesCache
    if (orderedValuesCacheCheck != null)
      return orderedValuesCacheCheck
    val orderedValuesResult = map.values.sortedBy { it.order }
    orderedValuesCache = orderedValuesResult
    return orderedValuesResult
  }
}

// ==================================================================
//
//
//
// ==================================================================

class SortedList<T : OrderedValue> {
  val list = mutableListOf<T>()

  val size get() = list.size

  val indices get() = list.indices

  fun isEmpty() = list.isEmpty()

  fun isNotEmpty() = list.isNotEmpty()

  fun getByIndex(index: Int) = list[index]

  fun getByOrder(order: Int) = list[findIndex(order)]

  fun getByIndexOrNull(index: Int) = list.getOrNull(index)

  fun getByOrderOrNull(order: Int) = list.getOrNull(findIndex(order))

  fun getByOrderOrPut(order: Int, factory: (order: Int) -> T): T {
    val index = findIndex(order)
    if (index >= 0)
      return list[index]
    val result = factory(order)
    list.add(-index - 1, result)
    return result
  }

  operator fun plusAssign(element: T) = Unit.also { add(element) }

  operator fun minusAssign(element: T) = Unit.also { remove(element) }

  operator fun contains(element: T) = findIndex(element.order) >= 0

  fun add(element: T) {
    val index = findIndex(element.order)
    if (index >= 0)
      throw IllegalStateException("element order already exists")
    list.add(-index - 1, element)
  }

  fun put(element: T) {
    val index = findIndex(element.order)
    if (index >= 0)
      list[index] = element
    else
      list.add(-index, element)
  }

  fun remove(order: Int): Boolean {
    val index = findIndex(order)
    if (index !in list.indices)
      return false
    list.removeAt(index)
    return true
  }

  fun remove(element: T): Boolean = remove(element.order)

  fun findIndex(order: Int): Int = list.binarySearch { it.order - order }

  fun clear() {
    list.clear()
  }

  inline fun forEach(block: (element: T) -> Unit) {
    for (index in indices) {
      block(list[index])
    }
  }
}


// ==================================================================
//
//
//
// ==================================================================

class UniqueList<T : Any> {
  val set = mutableSetOf<T>()
  val list = mutableListOf<T>()

  val indices get() = list.indices

  fun isEmpty() = list.isEmpty()

  fun isNotEmpty() = list.isNotEmpty()

  operator fun get(index: Int) = list[index]

  operator fun plusAssign(element: T) = Unit.also { add(element) }

  operator fun minusAssign(element: T) = Unit.also { remove(element) }

  operator fun contains(element: T) = element in set

  fun add(element: T): Boolean {
    if (set.add(element)) {
      list += element
      return true
    }
    return false
  }

  fun remove(element: T): Boolean {
    if (set.remove(element)) {
      list -= element
      return true
    }
    return false
  }

  fun removeLast(): T {
    val result = list.removeLast()
    set -= result
    return result
  }

  fun removeLastOrNull(): T? {
    val result = list.removeLastOrNull() ?: return null
    set -= result
    return result
  }

  fun clear() {
    set.clear()
    list.clear()
  }

  inline fun forEach(block: (element: T) -> Unit) {
    for (index in indices) {
      block(list[index])
    }
  }

  inline fun drainEach(block: (element: T) -> Unit) {
    forEach(block)
    clear()
  }
}


// ==================================================================
//
//
//
// ==================================================================

class BiDirectionalMap<K, V> {
  val keyToValue = linkedMapOf<K, V>()
  val valueToKey = linkedMapOf<V, K>()

  fun getValue(key: K): V? = keyToValue[key]

  fun getKey(value: V): K? = valueToKey[value]

  fun containsKey(key: K) = keyToValue.containsKey(key)

  fun containsValue(value: V) = valueToKey.containsKey(value)

  inline fun forEachKey(block: (key: K) -> Unit) = valueToKey.values.forEach(block)

  inline fun forEachValue(block: (value: V) -> Unit) = keyToValue.values.forEach(block)

  fun removeKey(key: K): V? {
    val value = keyToValue.remove(key)
    if (value != null) valueToKey.remove(value)
    return value
  }

  fun removeValue(value: V): K? {
    val key = valueToKey.remove(value)
    if (key != null) keyToValue.remove(key)
    return key
  }

  fun put(key: K, value: V) {
    removeKey(key)
    removeValue(value)
    keyToValue[key] = value
    valueToKey[value] = key
  }

  fun clear() {
    keyToValue.clear()
    valueToKey.clear()
  }
}
