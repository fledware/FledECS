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

class CachedArray<T : Any>(val builder: () -> Array<T>) {
  private var cache: Array<T>? = null

  operator fun invoke(): Array<T> {
    if (cache == null)
      cache = builder()
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

  fun clear() {
    set.clear()
    list.clear()
  }

  inline fun forEach(block: (element: T) -> Unit) {
    for (index in indices) {
      block(list[index])
    }
  }
}
