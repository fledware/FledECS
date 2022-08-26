package fledware.ecs

import fledware.ecs.ex.execute
import fledware.ecs.impl.DefaultEngine
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineEventsTest {
  private lateinit var engine: Engine
  private var onEngineStartCount: Int = 0
  private var onEngineShutdownCount: Int = 0
  private var onWorldCreatedCount: Int = 0
  private var onWorldDestroyedCount: Int = 0
  private var onBeforeUpdateCount: Int = 0
  private var onAfterUpdateCount: Int = 0

  @BeforeTest
  fun setupEngine() {
    engine = DefaultEngine()
    engine.events.onEngineStart += { onEngineStartCount++ }
    engine.events.onEngineShutdown += { onEngineShutdownCount++ }
    engine.events.onWorldCreated += { onWorldCreatedCount++ }
    engine.events.onWorldDestroyed += { onWorldDestroyedCount++ }
    engine.events.onBeforeUpdate += { onBeforeUpdateCount++ }
    engine.events.onAfterUpdate += { onAfterUpdateCount++ }
  }

  @Test
  fun engineEventsInitialState() {
    assertEquals(0, onEngineStartCount)
    assertEquals(0, onEngineShutdownCount)
    assertEquals(0, onWorldCreatedCount)
    assertEquals(0, onWorldDestroyedCount)
    assertEquals(0, onBeforeUpdateCount)
    assertEquals(0, onAfterUpdateCount)
  }

  @Test
  fun engineLifecycleEventsFire() {
    engine.start()
    assertEquals(1, onEngineStartCount)
    assertEquals(0, onEngineShutdownCount)
    assertEquals(0, onWorldCreatedCount)
    assertEquals(0, onWorldDestroyedCount)
    assertEquals(0, onBeforeUpdateCount)
    assertEquals(0, onAfterUpdateCount)
    engine.shutdown()
    assertEquals(1, onEngineStartCount)
    assertEquals(1, onEngineShutdownCount)
    assertEquals(0, onWorldCreatedCount)
    assertEquals(0, onWorldDestroyedCount)
    assertEquals(0, onBeforeUpdateCount)
    assertEquals(0, onAfterUpdateCount)
  }

  @Test
  fun worldLifecycleEventsFire() {
    engine.start()
    val world = engine.createTestWorld()
    assertEquals(1, onEngineStartCount)
    assertEquals(0, onEngineShutdownCount)
    assertEquals(1, onWorldCreatedCount)
    assertEquals(0, onWorldDestroyedCount)
    assertEquals(0, onBeforeUpdateCount)
    assertEquals(0, onAfterUpdateCount)
    engine.requestDestroyWorld(world.name)
    engine.handleRequests()
    assertEquals(1, onEngineStartCount)
    assertEquals(0, onEngineShutdownCount)
    assertEquals(1, onWorldCreatedCount)
    assertEquals(1, onWorldDestroyedCount)
    assertEquals(0, onBeforeUpdateCount)
    assertEquals(0, onAfterUpdateCount)
  }

  @Test
  fun updateLifecycleEventsFire() {
    engine.start()
    val world = engine.createTestWorld()
    world.execute {
      assertEquals(1, onEngineStartCount)
      assertEquals(0, onEngineShutdownCount)
      assertEquals(1, onWorldCreatedCount)
      assertEquals(0, onWorldDestroyedCount)
      assertEquals(1, onBeforeUpdateCount)
      assertEquals(0, onAfterUpdateCount)
    }
    engine.update(1f)
    assertEquals(1, onEngineStartCount)
    assertEquals(0, onEngineShutdownCount)
    assertEquals(1, onWorldCreatedCount)
    assertEquals(0, onWorldDestroyedCount)
    assertEquals(1, onBeforeUpdateCount)
    assertEquals(1, onAfterUpdateCount)
  }
}