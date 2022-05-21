package fledware.examples.gameoflife

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import driver.GameScreen
import driver.util.MouseInputProcessor
import driver.util.drawGrid
import driver.util.isKeyJustPressed
import driver.util.screenMax
import driver.util.screenMin
import driver.util.tinyLabelAndRow
import fledware.ecs.World
import fledware.ecs.entityComponentIndexOf
import fledware.ecs.forEach
import fledware.ecs.forEachWorld
import fledware.ecs.get
import fledware.utilities.get
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ktx.actors.txt
import ktx.scene2d.actors
import ktx.scene2d.table
import kotlin.system.measureTimeMillis

class GameOfLifeScreen : GameScreen() {

  // we only want to update every couple frames
  private val cellUpdateEvery = 0.1f
  private var nextUpdate: Float = cellUpdateEvery
  private var pauseSimulation = false

  // toggling cells
  private var lastCellToggled: Long = -1
  private val engineInfo = engine.data.contexts.get<EngineInfo>()
  private val isEven get() = engineInfo.isEven
  private var isLeftDragging = false

  // render stuffs
  private var drawGrid = true
  private val cellSize = 10
  private val cellSizeF = cellSize.toFloat()
  private val absoluteMax = engineInfo.maxCellLocation * cellSize
  private val viewportBounds = Rectangle()
  private val cellAliveIndex = engine.data.entityComponentIndexOf<CellAlive>()
  private val cellLocationIndex = engine.data.entityComponentIndexOf<CellLocation>()

  // ui stuffs
  private val fpsLabel: Label
  private val updateLabel: Label
  private val renderLabel: Label
  private val pausedLabel: Label
  private val drawGridLabel: Label

  // inputs stuffs
  var zoomSensitivity: Float = 0.1f
  var maxZoom: Float = 10f
  var minZoom: Float = 0.5f
  val mouse = MouseInputProcessor(viewport).also {
    it.onRightDrag += { _, dragDelta -> camera.translate(dragDelta) }
    it.onLeftDrag += { worldMousePos, dragDelta -> handleOnLeftDrag(worldMousePos, dragDelta) }
    it.onClick += { worldMousePos -> handleOnLeftDrag(worldMousePos, Vector2.Zero) }
    it.onScroll += { camera.zoom = MathUtils.clamp(camera.zoom + it * zoomSensitivity, minZoom, maxZoom) }
    Gdx.input.inputProcessor = it
  }

  private fun handleOnLeftDrag(worldMousePos: Vector2, dragDelta: Vector2) {
    if (!isLeftDragging) lastCellToggled = -1
    isLeftDragging = true
    val xLocation = ((worldMousePos.x - dragDelta.x) / cellSize).toInt()
    val yLocation = ((worldMousePos.y - dragDelta.y) / cellSize).toInt()
    val entity = engine.findEntityByLocation(xLocation, yLocation, engineInfo.worldSize)
    val entityId = entity?.id ?: -1
    if (entityId == lastCellToggled) return
    lastCellToggled = entityId
    entity?.get<CellAlive>()?.also {
      val isAlive = it.isAlive(isEven)
      it.setThisGen(isEven, !isAlive)
    }
  }

  init {
    camera.position.set(absoluteMax / 2f, absoluteMax / 2f, 0f)
    engine.data.forEachWorld { world ->
      val info = world.data.contexts.get<WorldInfo>()
      info.bounds.set(
          info.worldX * engineInfo.worldSize * cellSizeF,
          info.worldY * engineInfo.worldSize * cellSizeF,
          engineInfo.worldSize * cellSizeF,
          engineInfo.worldSize * cellSizeF,
      )
    }
    stage.actors {
      table(defaultSkin) {
        top()
        left()
        setFillParent(true)

        fpsLabel = tinyLabelAndRow("fps: ")
        updateLabel = tinyLabelAndRow("engine time: ")
        renderLabel = tinyLabelAndRow("engine render: ")
        pausedLabel = tinyLabelAndRow("paused (space): $pauseSimulation")
        drawGridLabel = tinyLabelAndRow("draw grid (G): $drawGrid")
        tinyLabelAndRow("fill random (F)")
        tinyLabelAndRow("reset (R)")
        tinyLabelAndRow("exit (esc)")
        tinyLabelAndRow("draw (left mouse)")
        tinyLabelAndRow("move camera (right mouse drag)")
        tinyLabelAndRow("zoom camera (middle mouse scroll)")
      }
    }
  }

  override fun render(delta: Float) {
    handleInputs()

    // update the engine only when needed
    handleEngineUpdate(delta)

    // update the camera
    camera.update()
    shapeRenderer.projectionMatrix = camera.combined

    // draw the squares that are alive and in the viewport
    renderWorlds()

    // draw the grid
    if (drawGrid) {
      shapeRenderer.drawGrid(viewport, cellSize, absoluteMinX = 0, absoluteMinY = 0,
                             absoluteMaxX = absoluteMax, absoluteMaxY = absoluteMax)
    }

    // draw the ui stage
    fpsLabel.txt = "fps: ${Gdx.graphics.framesPerSecond}"
    stage.act(delta)
    stage.draw()
  }

  private fun handleInputs() {
    if (isKeyJustPressed(Keys.R)) resetWorlds()
    if (isKeyJustPressed(Keys.F)) randomizeWorlds()
    if (isKeyJustPressed(Keys.G)) {
      drawGrid = !drawGrid
      drawGridLabel.txt = "draw grid (G): $drawGrid"
    }
    if (isKeyJustPressed(Keys.SPACE)) {
      pauseSimulation = !pauseSimulation
      pausedLabel.txt = "paused (space): $pauseSimulation"
    }
    if (isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit()
  }

  private fun handleEngineUpdate(delta: Float) {
    if (!mouse.isDragging) isLeftDragging = false
    if (!isLeftDragging && !pauseSimulation) {
      nextUpdate -= delta
      if (nextUpdate < 0) {
        nextUpdate += cellUpdateEvery
        val time = measureTimeMillis { engine.update(delta) }
        updateLabel.txt = "engine time ms: $time"
        engineInfo.isEven = !engineInfo.isEven
      }
    }
  }

  private fun renderWorlds() {
    val xMin = screenMin(camera.position.x, viewport.worldWidth, camera.zoom, cellSize, 0).toFloat()
    val xMax = screenMax(camera.position.x, viewport.worldWidth, camera.zoom, cellSize, absoluteMax).toFloat()
    val yMin = screenMin(camera.position.y, viewport.worldHeight, camera.zoom, cellSize, 0).toFloat()
    val yMax = screenMax(camera.position.y, viewport.worldHeight, camera.zoom, cellSize, absoluteMax).toFloat()
    viewportBounds.set(xMin, yMin, xMax - xMin + cellSize, yMax - yMin + cellSize)
    Gdx.gl.glLineWidth(0.5f)
    val time = measureTimeMillis {
      shapeRenderer.color = Color.WHITE
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
      engine.data.worlds.values.forEach { world ->
        val info = world.data.contexts.get<WorldInfo>()
        if (viewportBounds.overlaps(info.bounds)) world.render()
      }
      shapeRenderer.end()
    }
    renderLabel.txt = "render time ms: $time"
  }

  private fun World.render() {
    val cells = this.data.entityGroups["cells"] ?: throw IllegalStateException(
        "cells group not found for world $name")
    cells.forEach { entity ->
      if (!entity[cellAliveIndex].isAlive(isEven)) return@forEach
      val location = entity[cellLocationIndex]
      shapeRenderer.rect(location.x * cellSizeF, location.y * cellSizeF, cellSizeF, cellSizeF)
    }
  }

  private fun resetWorlds() {
    runBlocking {
      engine.data.worlds.values.forEach { world ->
        launch { world.reset() }
      }
    }
  }

  private fun World.reset() {
    val cells = this.data.entityGroups["cells"] ?: throw IllegalStateException(
        "cells group not found for world $name")
    cells.forEach { entity ->
      val alive = entity[cellAliveIndex]
      alive.evenAlive = false
      alive.oddAlive = false
    }
  }

  private fun randomizeWorlds() {
    runBlocking {
      engine.data.worlds.values.forEach { world ->
        launch { world.random() }
      }
    }
  }

  private fun World.random() {
    val cells = this.data.entityGroups["cells"] ?: throw IllegalStateException(
        "cells group not found for world $name")
    cells.forEach { entity ->
      val alive = entity[cellAliveIndex]
      alive.evenAlive = MathUtils.randomBoolean()
      alive.oddAlive = MathUtils.randomBoolean()
    }
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height)
  }
}

