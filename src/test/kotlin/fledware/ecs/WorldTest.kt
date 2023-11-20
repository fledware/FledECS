package fledware.ecs

import fledware.ecs.extensions.BlockExecutingSystem
import fledware.ecs.extensions.executeInUpdate
import fledware.ecs.internal.InternalSystemManager
import fledware.ecs.internal.findSystemsOrdered
import fledware.ecs.util.add
import fledware.ecs.world.DefaultWorld
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldTest {
  @Test
  fun canCreateWorld() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    assertEquals(3, world.entities.size)
    assertEquals(1, world.entityGroups.size)
    assertEquals(2, world.entityGroups.persons.size)
    assertNotNull(world.entities.map)
    assertEquals(2, world.systems.size)
  }

  @Test
  fun canRemoveAllEntities() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    assertEquals(3, world.removeEntities().size)
    assertEquals(0, world.entities.size)
    assertEquals(1, world.entityGroups.size)
    assertEquals(0, world.entityGroups.persons.size)
    assertEquals(2, world.systems.size)
  }

  @Test
  fun canDeleteAllEntities() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    world.deleteEntities()
    assertEquals(0, world.entities.size)
    assertEquals(1, world.entityGroups.size)
    assertEquals(0, world.entityGroups.persons.size)
    assertEquals(2, world.systems.size)
  }

  @Test
  fun canAddEntityOutsideUpdate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    val persons = world.entityGroups.persons
    assertEquals(2, persons.size)
    world.entities.createPersonEntity(5, 5)
    assertEquals(2, persons.size)
    world.update(1f)
    assertEquals(3, persons.size)
  }

  @Test
  fun canAddEntityDuringUpdate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    val persons = world.entityGroups.persons
    world.executeInUpdate {
      world.entities.createPersonEntity(5, 5)
    }
    assertEquals(2, persons.size)
    world.update(1f)
    assertEquals(3, persons.size)
  }

  @Test
  fun testLifecycleCalls() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    val system = world.systems.systems.get<MovementSystem>()
    assertEquals(1, system.onEnabledCount)
    assertEquals(0, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    world.update(1f)
    assertEquals(1, system.onEnabledCount)
    assertEquals(0, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(2, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    world.destroy()
    assertEquals(1, system.onEnabledCount)
    assertEquals(1, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(2, system.updateCount)
    assertEquals(1, system.onDestroyCount)
  }

  @Test
  fun testWorldAddSystemDuringUpdate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    val system = UpdateCountSystemNotMovement()
    world.executeInUpdate {
      this.world.systems.add(system)
      assertEquals(0, system.onCreateCount)
    }

    assertFalse(world.systems.systems.contains(UpdateCountSystemNotMovement::class))
    world.update(1f)
    assertTrue(world.systems.systems.contains(UpdateCountSystemNotMovement::class))
    assertEquals(1, system.onEnabledCount)
    assertEquals(0, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(0, system.updateCount)
    assertEquals(0, system.onDestroyCount)
    world.update(1f)
    assertEquals(1, system.onEnabledCount)
    assertEquals(0, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)
  }

  @Test
  fun testWorldRemoveSystemDuringUpdate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    val system = UpdateCountSystemNotMovement()
    world.systems.add(system)
    world.update(1f)
    assertEquals(1, system.onEnabledCount)
    assertEquals(0, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(1, system.updateCount)
    assertEquals(0, system.onDestroyCount)

    world.executeInUpdate {
      this.world.systems.remove(UpdateCountSystemNotMovement::class)
    }
    world.update(1f)
    assertEquals(1, system.onEnabledCount)
    assertEquals(1, system.onDisabledCount)
    assertEquals(1, system.onCreateCount)
    assertEquals(2, system.updateCount)
    assertEquals(1, system.onDestroyCount)
  }

  @Test
  fun entityEventsAreBuffered() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    var count = 0
    world.updateEvents.add<OnEntityUpdated> { count++ }
    world.update(1f)
    assertEquals(0, count)

    world.executeInUpdate { this.world.entities.map.notifyUpdate() }
    world.update(1f)
    assertEquals(1, count)

    world.executeInUpdate { this.world.entities.map.also { it.notifyUpdate(); it.notifyUpdate() } }
    world.update(1f)
    assertEquals(2, count)
  }

  @Test
  fun systemUpdatesAreSorted() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    var eventTriggers = 0
    world.updateEvents.add<OnSystemOrderChange> { eventTriggers++ }
    world.systems.systems.get<BlockExecutingSystem>().order = 10
    world.systems.systems.get<MovementSystem>().order = 5
    world.update(1f)
    assertEquals(2, eventTriggers)
    (world.systems as InternalSystemManager).findSystemsOrdered().also { systems ->
      assertIs<MovementSystem>(systems[0])
      assertIs<BlockExecutingSystem>(systems[1])
    }

    world.systems.systems.get<BlockExecutingSystem>().order = 3
    world.update(1f)
    assertEquals(3, eventTriggers)
    (world.systems as InternalSystemManager).findSystemsOrdered().also { systems ->
      assertIs<BlockExecutingSystem>(systems[0])
      assertIs<MovementSystem>(systems[1])
    }
  }

  @Test
  fun systemCanChangeOrderDuringUpdate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    world.systems.systems.get<BlockExecutingSystem>().order = 10
    world.systems.systems.get<MovementSystem>().order = 5
    world.update(1f)
    (world.systems as InternalSystemManager).findSystemsOrdered().also { systems ->
      assertIs<MovementSystem>(systems[0])
      assertIs<BlockExecutingSystem>(systems[1])
    }

    world.executeInUpdate { order = 3 }
    world.update(1f)
    (world.systems as InternalSystemManager).findSystemsOrdered().also { systems ->
      assertIs<BlockExecutingSystem>(systems[0])
      assertIs<MovementSystem>(systems[1])
    }
  }

  @Test
  fun systemCanRemoveSelfAfterCreate() {
    val world = DefaultWorld().worldBuilderMovementOnly()
    world.systems.add(ImmediateRemoveSystem())
    assertNotNull(world.systems.systems.getOrNull<ImmediateRemoveSystem>())
    world.update(1f)
    assertNull(world.systems.systems.getOrNull<ImmediateRemoveSystem>())
  }
}