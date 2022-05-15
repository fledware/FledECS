package driver

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import fledware.ecs.Engine
import fledware.ecs.ex.withEntityFlags
import fledware.ecs.ex.withWorldScenes
import fledware.ecs.impl.DefaultEngine
import fledware.ecs.impl.DefaultWorldUpdateStrategy
import fledware.ecs.impl.executorUpdateStrategy
import org.slf4j.LoggerFactory

val runner: LibgdxRunner
  get() = Gdx.app.applicationListener as LibgdxRunner

fun libgdxRun(builder: LibgdxRunner.() -> Screen) {
  val configuration = Lwjgl3ApplicationConfiguration()
  configuration.setWindowedMode(640 * 2, 480 * 2)
  Lwjgl3Application(LibgdxRunner(builder), configuration)
}

open class LibgdxRunner(val builder: LibgdxRunner.() -> Screen) : Game() {
  private val logger = LoggerFactory.getLogger(javaClass)
  lateinit var shapeRenderer: ShapeRenderer
  lateinit var spriteBatch: SpriteBatch
  lateinit var assetManager: AssetManager
  lateinit var engine: Engine

  override fun create() {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      println("unhandled global error")
      throwable.printStackTrace()
      if (Gdx.app != null)
        Gdx.app.exit()
      else
        Runtime.getRuntime().exit(1)
    }

    logger.info("create()")
    spriteBatch = SpriteBatch()
    shapeRenderer = ShapeRenderer()
    assetManager = AssetManager()
    engine = DefaultEngine(executorUpdateStrategy())
        .withEntityFlags()
        .withWorldScenes()
    engine.start()
    screen = builder()
  }

  override fun render() {
    ScreenUtils.clear(0f, 0f, 0f, 1f)
    super.render()
  }

  override fun dispose() {
    super.dispose()
    if (this::spriteBatch.isInitialized)
      spriteBatch.dispose()
    if (this::shapeRenderer.isInitialized)
      shapeRenderer.dispose()
    if (this::assetManager.isInitialized)
      assetManager.dispose()
    if (this::engine.isInitialized)
      engine.shutdown()
  }
}
