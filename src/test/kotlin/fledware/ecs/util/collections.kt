package fledware.ecs.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private data class SortedValue(
    override val order: Int
) : OrderedValue

class SortedListTest {
  @Test
  fun testCanInsert() {
    val list = SortedList<SortedValue>()
    assertEquals(0, list.size)
    list.add(SortedValue(12))
    assertEquals(1, list.size)
  }

  @Test
  fun testCanInsert2() {
    val list = SortedList<SortedValue>()
    assertEquals(0, list.size)
    list.add(SortedValue(12))
    list.add(SortedValue(14))
    assertEquals(2, list.size)
  }

  @Test
  fun canGetByIndex() {
    val list = SortedList<SortedValue>()
    assertNull(list.getByIndexOrNull(0))
    list.add(SortedValue(12))
    assertEquals(SortedValue(12), list.getByIndexOrNull(0))
  }

  @Test
  fun canGetByIndex2() {
    val list = SortedList<SortedValue>()
    assertNull(list.getByIndexOrNull(0))
    list.add(SortedValue(14))
    list.add(SortedValue(12))
    list.add(SortedValue(2))
    list.add(SortedValue(21))
    list.add(SortedValue(5))
    list.add(SortedValue(13))
    list.add(SortedValue(99))
    list.add(SortedValue(-5))
    assertEquals(SortedValue(-5), list.getByIndexOrNull(0))
    assertEquals(SortedValue(2), list.getByIndexOrNull(1))
    assertEquals(SortedValue(5), list.getByIndexOrNull(2))
    assertEquals(SortedValue(12), list.getByIndexOrNull(3))
    assertEquals(SortedValue(13), list.getByIndexOrNull(4))
    assertEquals(SortedValue(14), list.getByIndexOrNull(5))
    assertEquals(SortedValue(21), list.getByIndexOrNull(6))
    assertEquals(SortedValue(99), list.getByIndexOrNull(7))
  }

  @Test
  fun canGetByOrder() {
    val list = SortedList<SortedValue>()
    assertNull(list.getByOrderOrNull(12))
    list.add(SortedValue(12))
    assertEquals(SortedValue(12), list.getByOrderOrNull(12))
  }

  @Test
  fun canGetByOrder2() {
    val list = SortedList<SortedValue>()
    assertNull(list.getByIndexOrNull(0))
    list.add(SortedValue(14))
    list.add(SortedValue(12))
    list.add(SortedValue(2))
    list.add(SortedValue(21))
    list.add(SortedValue(5))
    list.add(SortedValue(13))
    list.add(SortedValue(99))
    list.add(SortedValue(-5))
    assertEquals(SortedValue(-5), list.getByOrderOrNull(-5))
    assertEquals(SortedValue(2), list.getByOrderOrNull(2))
    assertEquals(SortedValue(5), list.getByOrderOrNull(5))
    assertEquals(SortedValue(12), list.getByOrderOrNull(12))
    assertEquals(SortedValue(13), list.getByOrderOrNull(13))
    assertEquals(SortedValue(14), list.getByOrderOrNull(14))
    assertEquals(SortedValue(21), list.getByOrderOrNull(21))
    assertEquals(SortedValue(99), list.getByOrderOrNull(99))
  }

  @Test
  fun canGetByOrderOrPut() {
    val list = SortedList<SortedValue>()
    assertNull(list.getByOrderOrNull(12))
    list.getByOrderOrPut(12) { SortedValue(it) }
    assertEquals(SortedValue(12), list.getByOrderOrNull(12))
    list.getByOrderOrPut(12) { SortedValue(it) }
  }
}
