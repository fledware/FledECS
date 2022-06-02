package fledware.ecs

import fledware.ecs.ex.BlockExecutingSystem
import fledware.ecs.ex.InitSystem
import fledware.ecs.ex.execute
import fledware.ecs.ex.initWith
import fledware.ecs.impl.AbstractWorldData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldTest {
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
    val system = UpdateCountSystemNotMovement()
    world.execute {
      data.addSystem(system)
      assertEquals(0, system.onCreateCount)
    }

    assertFalse(world.data.systems.contains(UpdateCountSystemNotMovement::class))
    engine.update(1f)
    assertTrue(world.data.systems.contains(UpdateCountSystemNotMovement::class))
    assertEquals(0, system.onCreateCount)
    assertEquals(0, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)
  }

  @Test
  fun testWorldRemoveSystemDuringUpdate() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()

    val system = UpdateCountSystemNotMovement()
    world.data.addSystem(system)
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)

    world.execute {
      data.removeSystem(UpdateCountSystemNotMovement::class)
    }
    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)

    engine.update(1f)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(1, system.onDestroyCount)
  }

  @Test
  fun entityEventsAreBuffered() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    var count = 0
    world.events.onEntityChanged += { count++ }
    engine.update(1f)
    assertEquals(0, count)

    world.execute { data.entities.first().notifyUpdate() }
    engine.update(1f)
    assertEquals(1, count)

    world.execute { data.entities.first().also { it.notifyUpdate(); it.notifyUpdate() } }
    engine.update(1f)
    assertEquals(2, count)
  }

  @Test
  fun systemUpdatesAreSorted() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    world.data.systems.get<BlockExecutingSystem>().order = 10
    world.data.systems.get<MovementSystem>().order = 5

    engine.update(1f)
    (world.data as AbstractWorldData).systemsList.also { systems ->
      assertIs<MovementSystem>(systems[0])
      assertIs<BlockExecutingSystem>(systems[1])
    }

    world.data.systems.get<BlockExecutingSystem>().order = 3
    engine.update(1f)
    (world.data as AbstractWorldData).systemsList.also { systems ->
      assertIs<BlockExecutingSystem>(systems[0])
      assertIs<MovementSystem>(systems[1])
    }
  }

  @Test
  fun systemCanChangeOrderDuringUpdate() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    world.data.systems.get<BlockExecutingSystem>().order = 10
    world.data.systems.get<MovementSystem>().order = 5

    engine.update(1f)
    (world.data as AbstractWorldData).systemsList.also { systems ->
      assertIs<MovementSystem>(systems[0])
      assertIs<BlockExecutingSystem>(systems[1])
    }

    world.execute { order = 3 }
    engine.update(1f)
    (world.data as AbstractWorldData).systemsList.also { systems ->
      assertIs<BlockExecutingSystem>(systems[0])
      assertIs<MovementSystem>(systems[1])
    }
  }

  @Test
  fun systemCanRemoveSelfAfterCreate() {
    var yayISay: String? = null
    val engine = createTestEngine()
    val world = engine.createWorldAndFlush("test") {
      worldBuilderMovementOnly()
      initWith { _, _ ->
        yayISay = "yay!!!"
      }
    }

    assertEquals("yay!!!", yayISay)
    assertNull(world.data.systems.getOrNull<InitSystem>())
  }
}