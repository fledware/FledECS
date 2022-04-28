package fledware.ecs.util


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
