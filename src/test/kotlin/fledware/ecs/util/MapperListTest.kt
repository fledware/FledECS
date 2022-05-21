package fledware.ecs.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MapperListTest {
  val mapper = Mapper<Class<*>>()
  val list = mapper.list<SomeInterface>()
  val indexA = mapper.indexOf<SomeA>(SomeA::class.java)
  val indexB = mapper.indexOf<SomeB>(SomeB::class.java)
  val indexC = mapper.indexOf<SomeC>(SomeC::class.java)
  val indexI = mapper.indexOf<SomeInterface>(SomeInterface::class.java)
  
  @Test
  fun get() {
    assertThrows(IndexOutOfBoundsException::class.java) {
      list.getByIndex(indexA)
    }
    list.setByIndex(indexA, SomeA())
    assertNotNull(list.getByIndex(indexA))
    assertThrows(IndexOutOfBoundsException::class.java) {
      list.getByIndex(indexB)
    }
  }
  
  @Test
  fun getOrNull() {
    assertNull(list.getByIndexOrNull(indexA))
    list.setByIndex(indexA, SomeA())
    assertNotNull(list.getByIndexOrNull(indexA))
    assertNull(list.getByIndexOrNull(indexB))
  }
  
  @Test
  fun getOrDefault() {
    val first = SomeA()
    val second = SomeA()
    assertSame(first, list.getByIndexOrDefault(indexA, first))
    list.setByIndex(indexA, second)
    assertSame(second, list.getByIndexOrDefault(indexA, first))
  }
  
  @Test
  fun getOrSet() {
    val first = SomeA()
    assertNull(list.getByIndexOrNull(indexA))
    assertSame(first, list.getByIndexOrSet(indexA, first))
    assertSame(first, list.getByIndexOrNull(indexA))
  }
  
  @Test
  fun getOrCreate() {
    val first = SomeA()
    assertNull(list.getByIndexOrNull(indexA))
    assertSame(first, list.getByIndexOrCreate(indexA) { first })
    assertSame(first, list.getByIndexOrNull(indexA))
  }
  
  @Test
  fun contains() {
    assertFalse(list.containsIndex(indexA))
    list.setByIndex(indexA, SomeA())
    assertTrue(list.containsIndex(indexA))
    list.setByIndex(indexA, null)
    assertFalse(list.containsIndex(indexA))
  }
  
  @Test
  fun set() {
    val first = SomeA()
    val second = SomeA()
    assertNull(list.getByIndexOrNull(indexA))
    list.setByIndex(indexA, first)
    assertSame(first, list.getByIndex(indexA))
    list.setByIndex(indexA, second)
    assertSame(second, list.getByIndex(indexA))
  }
  
  @Test
  fun setIfNull() {
    val first = SomeA()
    val second = SomeA()
    assertNull(list.getByIndexOrNull(indexA))
    assertSame(first, list.setByIndexIfNull(indexA, first))
    assertSame(first, list.getByIndexOrNull(indexA))
    assertSame(first, list.setByIndexIfNull(indexA, second))
    assertSame(first, list.getByIndexOrNull(indexA))
  }
  
  @Test
  fun setOrThrow() {
    list.setByIndexOrThrow(indexA, SomeA())
    assertThrows(IllegalStateException::class.java) {
      list.setByIndexOrThrow(indexA, SomeA())
    }
  }
  
  @Test
  fun multipleSets() {
    val someA = SomeA()
    val someB = SomeB()
    val someC = SomeC()
    assertNull(list.getByIndexOrNull(indexA))
    assertNull(list.getByIndexOrNull(indexB))
    assertNull(list.getByIndexOrNull(indexC))
    list.setByIndex(indexA, someA)
    assertSame(someA, list.getByIndexOrNull(indexA))
    assertNull(list.getByIndexOrNull(indexB))
    assertNull(list.getByIndexOrNull(indexC))
    list.setByIndex(indexB, someB)
    assertSame(someA, list.getByIndexOrNull(indexA))
    assertSame(someB, list.getByIndexOrNull(indexB))
    assertNull(list.getByIndexOrNull(indexC))
    list.setByIndex(indexC, someC)
    assertSame(someA, list.getByIndexOrNull(indexA))
    assertSame(someB, list.getByIndexOrNull(indexB))
    assertSame(someC, list.getByIndexOrNull(indexC))
  }

  @Test
  fun canSetInterface() {
    val someA = SomeA()
    assertNull(list.getByIndexOrNull(indexA))
    assertNull(list.getByIndexOrNull(indexB))
    assertNull(list.getByIndexOrNull(indexC))
    assertNull(list.getByIndexOrNull(indexI))
    list.setByIndexOrThrow(indexI, someA)
    assertNull(list.getByIndexOrNull(indexA))
    assertNull(list.getByIndexOrNull(indexB))
    assertNull(list.getByIndexOrNull(indexC))
    assertSame(someA, list.getByIndexOrNull(indexI))
  }
}