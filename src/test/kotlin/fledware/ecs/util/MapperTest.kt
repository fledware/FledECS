package fledware.ecs.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


internal class MapperTest {
  @Test
  fun getObjectReuse() {
    val mapper = Mapper<String>()
    assertEquals(0, mapper["hello"])
    assertEquals(0, mapper["hello"])
    assertEquals(1, mapper["world"])
    assertEquals(1, mapper["world"])
    assertEquals(0, mapper["hello"])
    assertEquals(1, mapper["world"])
  }
  
  @Test
  fun reverseLookup() {
    val mapper = Mapper<String>()
    assertEquals(0, mapper["hello"])
    assertEquals(1, mapper["world"])
    
    assertEquals("hello", mapper.reverseLookup(0))
    assertEquals("world", mapper.reverseLookup(1))
  }

  @Test
  fun getMapsKeyOrder() {
    val mapper = Mapper<String>()
    assertEquals(0, mapper["key0"])
    assertEquals(1, mapper["key1"])
    assertEquals(2, mapper["key2"])
    assertEquals(3, mapper["key3"])
    assertEquals(4, mapper["key4"])
    assertEquals(0, mapper["key0"])
    assertEquals(1, mapper["key1"])
    assertEquals(2, mapper["key2"])
    assertEquals(3, mapper["key3"])
    assertEquals(4, mapper["key4"])
  }
}
