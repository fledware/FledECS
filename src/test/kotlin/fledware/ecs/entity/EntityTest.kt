package fledware.ecs.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EntityTest {
  @Test
  fun canGetTypeIdAndIndexId() {
    val entity = Entity.of(12, 35)

    assertEquals(12, entity.typeId)
    assertEquals(35, entity.indexId)
  }

  @Test
  fun throwsWhenInvalidTypeId() {
    assertFailsWith<IllegalArgumentException> {
      Entity.of(-1, 12)
    }.also { exception ->
      assertEquals("invalid type", exception.message)
    }

    assertFailsWith<IllegalArgumentException> {
      Entity.of(200, 12)
    }.also { exception ->
      assertEquals("invalid type", exception.message)
    }
  }

  @Test
  fun throwsWhenInvalidIndexId() {
    assertFailsWith<IllegalArgumentException> {
      Entity.of(2, -321)
    }.also { exception ->
      assertEquals("invalid index", exception.message)
    }

    assertFailsWith<IllegalArgumentException> {
      Entity.of(2, 2_000_000_000)
    }.also { exception ->
      assertEquals("invalid index", exception.message)
    }
  }
}