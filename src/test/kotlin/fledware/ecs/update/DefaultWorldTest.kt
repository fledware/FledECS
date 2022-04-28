package fledware.ecs.update

import fledware.ecs.MovementSystem
import fledware.ecs.UpdateCountSystem
import fledware.ecs.createPersonEntity
import fledware.ecs.createTestEngine
import fledware.ecs.createTestWorld
import fledware.ecs.ex.BlockExecutingSystem
import fledware.ecs.ex.execute
import fledware.ecs.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultWorldTest {
  @Test
  fun testEntitiesOwningWorld() {
    val engine = createTestEngine()
    val world1 = engine.createTestWorld("world1")
    val world2 = engine.createTestWorld("world2")
    val entities1 = world1.data.systems[MovementSystem::class].entities.entities
    val entities2 = world2.data.systems[MovementSystem::class].entities.entities
    val entity1 = entities1.find { it.name == "target" }!!
    val entity2 = entities2.find { it.name == "target" }!!
    assertEquals("world1", entity1.world)
    assertEquals("world2", entity2.world)
    engine.update(1f)
    assertEquals("world1", entity1.world)
    assertEquals("world2", entity2.world)
  }

  @Test
  fun entityPassingThrowsOnNameCollision() {
    val engine = createTestEngine()
    val world1 = engine.createTestWorld("world1")
    val world2 = engine.createTestWorld("world2")
    assertEquals(3, world1.data.entities.size())
    assertEquals(3, world2.data.entities.size())
    engine.update(1f)

    world1.data.systems.get<BlockExecutingSystem>().execute {
      val passing = data.entities.find { it.name == "target" }!!
      data.sendEntity("world2", passing)
    }
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

  @Test
  fun testWorldClear() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    engine.update(1f)

    assertEquals(3, world.data.entities.size())
    assertEquals(1, world.data.entityGroups.size)
    assertEquals(2, world.data.entityGroups.values.first().size)
    world.data.createPersonEntity(3, 3)
    engine.update(1f)
    assertEquals(4, world.data.entities.size())
    assertEquals(1, world.data.entityGroups.size)
    assertEquals(3, world.data.entityGroups.values.first().size)

    world.data.clearEntities()
    assertEquals(0, world.data.entities.size())
    assertEquals(1, world.data.entityGroups.size)
    assertEquals(0, world.data.entityGroups.values.first().size)
  }

  @Test
  fun testLifecycleCalls() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    val system = world.data.systems.get<MovementSystem>()
    assertEquals(1, system.onCreateCount)
    assertEquals(0, system.onDestroyCount)
    assertEquals(0, system.updateCount)
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    engine.requestDestroyWorld(world.name)
    engine.handleRequests()
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(1, system.onDestroyCount)
  }

  @Test
  fun testWorldAddSystemDuringUpdate() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    val system = UpdateCountSystem()
    world.execute {
      data.addSystem(system)
      assertEquals(0, system.onCreateCount)
    }
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(0, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)
  }
}