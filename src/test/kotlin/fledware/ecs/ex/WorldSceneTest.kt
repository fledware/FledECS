package fledware.ecs.ex

import fledware.ecs.MapDimensions
import fledware.ecs.createEmptySceneWorld
import fledware.ecs.createPersonEntity
import fledware.ecs.createTestEngine
import fledware.ecs.createTestWorld
import fledware.ecs.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorldSceneTest {
  @Test
  fun testSceneSize() {
    val engine = createTestEngine().withEntityFlags().withWorldScenes()
    val world = engine.createTestWorld()
    engine.update(1f)

    assertEquals(1, world.data.contexts.size)
    assertEquals(3, world.data.entities.size())
    world.data.createEntity {  }
    engine.update(1f)
    assertEquals(4, world.data.entities.size())
  }

  @Test
  fun testSceneClear() {
    val engine = createTestEngine().withEntityFlags().withWorldScenes()
    val world = engine.createTestWorld()
    engine.update(1f)

    world.data.sceneName = "hello"
    assertEquals(1, world.data.contexts.size)
    assertEquals(3, world.data.entities.size())

    val scene = world.data.clearScene()
    assertEquals(0, world.data.entities.size())
    assertEquals("", world.data.sceneName)
    assertEquals(3, scene.entities.size)
    assertEquals("hello", scene.name)
  }

  @Test
  fun testSceneName() {
    val engine = createTestEngine().withEntityFlags().withWorldScenes()
    val world = engine.createTestWorld()
    engine.update(1f)

    assertFalse(world.data.hasScene)
    world.data.sceneName = "hello"
    assertTrue(world.data.hasScene)
  }

  @Test
  fun testSceneLoad() {
    val engine = createTestEngine().withEntityFlags().withWorldScenes()
    val world = engine.createEmptySceneWorld()
    engine.update(1f)

    val scene = engine.data.scenes.createScene("yay") {
      createEntity("map") {
        add(MapDimensions(10, 10))
      }
      repeat(10) {
        createPersonEntity(it, it)
      }
    }

    assertEquals(0, world.data.entities.size())
    assertFalse(world.data.hasScene)
    world.data.importScene(scene)

    engine.update(1f)
    assertTrue(world.data.hasScene)
    assertEquals("yay", world.data.sceneName)
    assertEquals(11, world.data.entities.size())
    assertNotNull(world.data.map)
  }

  @Test
  fun testSceneUnload() {
    val engine = createTestEngine().withEntityFlags().withWorldScenes()
    val world = engine.createEmptySceneWorld()
    engine.update(1f)

    assertFalse(world.data.hasScene)
    world.data.createScene("yay") {
      createEntity("map") {
        add(MapDimensions(10, 10))
      }
      repeat(10) {
        createPersonEntity(it, it)
      }
    }
    engine.update(1f)
    assertTrue(world.data.hasScene)

    val scene = world.data.clearScene()
    engine.update(1f)

    assertFalse(world.data.hasScene)
    assertEquals("yay", scene.name)
    assertEquals(11, scene.entities.size)
  }

  @Test
  fun testSceneFactory() {
    val engine = createTestEngine().withEntityCaching().withEntityFlags().withWorldScenes()
    engine.data.scenes.saveSceneFactory("stuffs") {
      createEntity("map") {
        add(MapDimensions(10, 10))
      }
      repeat(10) {
        createPersonEntity(it, it)
      }
    }
    val world = engine.createEmptySceneWorld()
    world.data.importSceneFromFactory("stuffs")
    engine.update(1f)

    assertEquals(11, world.data.entities.size())
  }
}