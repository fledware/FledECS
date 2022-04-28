package fledware.ecs.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MapperListTest {
  val mapper = Mapper<Class<*>>()
  val list = mapper.list<SomeInterface>()
  val indexA = mapper.indexOf<SomeA>(SomeA::class.java)
  val indexB = mapper.indexOf<SomeB>(SomeB::class.java)
  val indexC = mapper.indexOf<SomeC>(SomeC::class.java)
  
  @Test
  fun get() {
    assertThrows(IndexOutOfBoundsException::class.java) {
      list[indexA]
    }
    list[indexA] = SomeA()
    assertNotNull(list[indexA])
    assertThrows(IndexOutOfBoundsException::class.java) {
      list[indexB]
    }
  }
  
  @Test
  fun getOrNull() {
    assertNull(list.getOrNull(indexA))
    list[indexA] = SomeA()
    assertNotNull(list.getOrNull(indexA))
    assertNull(list.getOrNull(indexB))
  }
  
  @Test
  fun getOrDefault() {
    val first = SomeA()
    val second = SomeA()
    assertSame(first, list.getOrDefault(indexA, first))
    list[indexA] = second
    assertSame(second, list.getOrDefault(indexA, first))
  }
  
  @Test
  fun getOrSet() {
    val first = SomeA()
    assertNull(list.getOrNull(indexA))
    assertSame(first, list.getOrSet(indexA, first))
    assertSame(first, list.getOrNull(indexA))
  }
  
  @Test
  fun getOrCreate() {
    val first = SomeA()
    assertNull(list.getOrNull(indexA))
    assertSame(first, list.getOrCreate(indexA) { first })
    assertSame(first, list.getOrNull(indexA))
  }
  
  @Test
  fun contains() {
    assertFalse(indexA in list)
    list[indexA] = SomeA()
    assertTrue(indexA in list)
    list[indexA] = null
    assertFalse(indexA in list)
  }
  
  @Test
  fun set() {
    val first = SomeA()
    val second = SomeA()
    assertNull(list.getOrNull(indexA))
    list[indexA] = first
    assertSame(first, list[indexA])
    list[indexA] = second
    assertSame(second, list[indexA])
  }
  
  @Test
  fun setIfNull() {
    val first = SomeA()
    val second = SomeA()
    assertNull(list.getOrNull(indexA))
    assertSame(first, list.setIfNull(indexA, first))
    assertSame(first, list.getOrNull(indexA))
    assertSame(first, list.setIfNull(indexA, second))
    assertSame(first, list.getOrNull(indexA))
  }
  
  @Test
  fun setOrThrow() {
    list.setOrThrow(indexA, SomeA())
    assertThrows(IllegalStateException::class.java) {
      list.setOrThrow(indexA, SomeA())
    }
  }
  
  @Test
  fun multipleSets() {
    val someA = SomeA()
    val someB = SomeB()
    val someC = SomeC()
    assertNull(list.getOrNull(indexA))
    assertNull(list.getOrNull(indexB))
    assertNull(list.getOrNull(indexC))
    list[indexA] = someA
    assertSame(someA, list.getOrNull(indexA))
    assertNull(list.getOrNull(indexB))
    assertNull(list.getOrNull(indexC))
    list[indexB] = someB
    assertSame(someA, list.getOrNull(indexA))
    assertSame(someB, list.getOrNull(indexB))
    assertNull(list.getOrNull(indexC))
    list[indexC] = someC
    assertSame(someA, list.getOrNull(indexA))
    assertSame(someB, list.getOrNull(indexB))
    assertSame(someC, list.getOrNull(indexC))
  }
}