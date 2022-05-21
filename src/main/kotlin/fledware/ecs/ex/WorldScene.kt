package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.EngineData
import fledware.ecs.Entity
import fledware.ecs.EntityFactory
import fledware.ecs.WorldBuilder
import fledware.ecs.WorldData
import fledware.utilities.get
import fledware.utilities.getMaybe
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


// ==================================================================
//
// engine level
//
// ==================================================================

fun Engine.withWorldScenes(): Engine {
  data.contexts.put(WorldSceneEngineApi(this))
  addCreateWorldDecorator {
    components.put(WorldSceneData(""))
  }
  return this
}

val EngineData.scenes: WorldSceneEngineApi
  get() = contexts.get()

// ==================================================================
//
// scene main api
//
// ==================================================================

data class WorldSceneEngineApi(private val engine: Engine) {
  val savedScenes = ConcurrentHashMap<String, Scene>()
  val sceneFactories = ConcurrentHashMap<String, SceneFactoryWrapper>()

  /**
   * creates a scene that doesn't belong to any world. This scene can
   * be loaded into worlds and used to manage multiple different scenes.
   */
  fun createScene(name: String, builder: SceneFactory.() -> Unit): Scene {
    val factory = SceneFactory(engine, name)
    factory.builder()
    return factory.build()
  }

  /**
   * creates a scene factory and saves it. You can then get a fresh
   * copy of it by calling factoryScene with the same name.
   */
  fun saveSceneFactory(name: String, builder: SceneFactory.() -> Unit) {
    sceneFactories[name] = SceneFactoryWrapper(engine, name, builder)
  }

  /**
   * creates a fresh copy of a scene saved with saveSceneFactory
   */
  fun factoryScene(name: String): Scene {
    val factory = sceneFactories[name] ?: throw IllegalStateException("scene not found: $name")
    return factory.factory()
  }

  /**
   * saves the given scene
   */
  fun saveScene(scene: Scene, overrideOk: Boolean = false) {
    savedScenes.compute(scene.name) { _, current ->
      if (!overrideOk && current != null)
        throw IllegalStateException("scene with name already exists: ${scene.name}")
      scene
    }
  }

  /**
   * removes a saved scene and returns it.
   */
  fun removeSavedScene(name: String): Scene {
    return removeSavedSceneMaybe(name)
        ?: throw IllegalStateException("no saved scene with name: $name")
  }

  /**
   * variant of removeSavedScene that will return null if a saved scene with
   * the given name doesn't exist.
   */
  fun removeSavedSceneMaybe(name: String): Scene? {
    return savedScenes.remove(name)
  }
}

class SceneFactoryWrapper(private val engine: Engine,
                          private val name: String,
                          private val builder: SceneFactory.() -> Unit) {
  fun factory(): Scene {
    val factory = SceneFactory(engine, name)
    factory.builder()
    return factory.build()
  }
}

class SceneFactory(override val engine: Engine,
                   val name: String)
  : EntityFactory {

  private val entities = mutableListOf<Entity>()

  override fun importEntity(entity: Entity) {
    entities += entity
  }

  override fun createEntity(decorator: Entity.() -> Unit): Entity {
    val result = engine.data.createEntity(decorator)
    entities.add(result)
    return result
  }

  fun build() = Scene(name, entities)
}

// ==================================================================
//
// world builder level
//
// ==================================================================

/**
 * gets the scene data specific to this world
 */
val WorldBuilder.sceneData: WorldSceneData
  get() = components.getMaybe() ?: throw IllegalStateException("no scene data found")

/**
 * returns the current scene name.
 *
 * empty string means no scene.
 */
var WorldBuilder.sceneName: String
  get() = sceneData.name
  set(value) {
    sceneData.name = value
  }

val WorldBuilder.isWorldSceneEnabled: Boolean
  get() = components.getMaybe<WorldSceneData>() != null

// ==================================================================
//
// world level
//
// ==================================================================

/**
 * Represents a scene that can be loaded and unloaded.
 */
data class Scene(val name: String,
                 val entities: List<Entity>)

/**
 * scene data component put on the entity
 */
data class WorldSceneData(var name: String)

/**
 * gets the scene data specific to this world
 */
val WorldData.sceneData: WorldSceneData
  get() = contexts.getMaybe() ?: throw IllegalStateException("no scene data found")

/**
 * returns the current scene name.
 *
 * empty string means no scene.
 */
var WorldData.sceneName: String
  get() = sceneData.name
  set(value) {
    sceneData.name = value
  }

/**
 * returns if there is a current scene loaded.
 */
val WorldData.hasScene: Boolean
  get() = sceneName.isNotEmpty()

/**
 * clears the current scene and returns a WorldScene
 * object to allow it to be loaded again.
 */
fun WorldData.clearScene(): Scene {
  return clearSceneMaybe() ?: throw IllegalStateException("no scene to clear")
}

/**
 * variant of `clearScene` that doesn't throw an exception if
 * no scene is loaded.
 */
fun WorldData.clearSceneMaybe(): Scene? {
  if (!hasScene)
    return null
  val result = Scene(sceneName, clearEntities())
  sceneName = ""
  return result
}

/**
 * Clears the scene and sends the entities to the cache
 */
fun WorldData.clearSceneToCache() {
  sceneName = ""
  clearEntitiesToCache()
}

/**
 * Clears the scene and sends it to the engine saves.
 */
fun WorldData.clearSceneAndSave(saveOverrideOk: Boolean = false) {
  engine.data.scenes.saveScene(clearScene(), saveOverrideOk)
}

/**
 * Removes the scene from the engine saves and imports it.
 */
fun WorldData.importSceneFromSaves(name: String) {
  importScene(engine.data.scenes.removeSavedScene(name))
}

/**
 * Creates a new scene from the named factory and imports it.
 */
fun WorldData.importSceneFromFactory(name: String) {
  importScene(engine.data.scenes.factoryScene(name))
}

/**
 *
 */
fun WorldData.importScene(scene: Scene) {
  if (hasScene)
    throw IllegalStateException("already has scene: $sceneName")
  if (entities.size() > 0)
    throw IllegalStateException("world not empty: ${entities.size()}")
  sceneName = scene.name
  importEntities(scene.entities)
}

/**
 *
 */
fun WorldData.createScene(name: String, builder: WorldData.() -> Unit) {
  if (hasScene)
    throw IllegalStateException("already has scene: $sceneName")
  if (entities.size() > 0)
    throw IllegalStateException("world not empty: ${entities.size()}")
  builder()
  sceneName = name
}
