package fledware.ecs

import fledware.ecs.ex.BlockExecutingSystem
import fledware.ecs.ex.execute
import fledware.ecs.ex.withEntityCaching
import fledware.ecs.ex.withEntityFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MultiWorldTest {

  private fun Engine.setupWorlds(): Engine {
    repeat(10) { index: Int ->
      this.requestCreateWorld("world-$index") {
        worldBuilderMovementOnlyIndexed(index)
      }
    }
    this.handleRequests()
    return this
  }

  @Test
  fun testWorldsCanUpdate() {
    val engine = createTestEngine().withEntityCaching().withEntityFlags().setupWorlds()
    assertEquals(10, engine.data.worlds.size)
    engine.update(1f)
    repeat(10) { index: Int ->
      val world = engine.data.worlds["world-$index"]
      assertNotNull(world)
      val entity = world.data.entitiesNamed["target"]
      assertNotNull(entity)
      assertTrue(entity.isPersonEntity())
      entity[Movement::class].deltaY = 1
      assertEquals(index, entity[Placement::class].x)
      assertEquals(0, entity[Placement::class].y)

    }
    engine.update(1f)
    repeat(10) { index: Int ->
      val entity = engine.data.worlds["world-$index"]!!.data.entitiesNamed["target"]!!
      assertEquals(index, entity[Placement::class].x)
      assertEquals(1, entity[Placement::class].y)
    }
  }

  @Test
  fun entityPassingThrowsOnNameCollision() {
    val engine = createTestEngine()
    val world1 = engine.createTestWorld("world1")
    val world2 = engine.createTestWorld("world2")
    assertEquals(3, world1.data.entities.size())
    assertEquals(3, world2.data.entities.size())
    engine.update(1f)

    world1.execute { data.sendEntity("world2", data.entitiesNamed["target"]!!) }
    engine.update(1f)

    assertEquals(2, world1.data.entities.size())
    assertEquals(3, world2.data.entities.size())
    val exception = assertFailsWith<IllegalStateException> {
      engine.update(1f)
    }
    assertEquals("named entity already exists: target", exception.message)
  }

  @Test
  fun testEntityPassing() {
    val engine = createTestEngine()
    val world1 = engine.createTestWorld("world1")
    val world2 = engine.createTestWorld("world2")
    assertEquals(3, world1.data.entities.size())
    assertEquals(3, world2.data.entities.size())
    engine.update(1f)

    world1.data.systems[BlockExecutingSystem::class].execute {
      val passing = data.entities.find { it.name == "target" }!!
      passing.name = "other"
      data.sendEntity("world2", passing)
    }
    engine.update(1f)
    assertEquals(2, world1.data.entities.size())
    assertEquals(3, world2.data.entities.size())
    engine.update(1f)
    assertEquals(2, world1.data.entities.size())
    assertEquals(4, world2.data.entities.size())
  }
}