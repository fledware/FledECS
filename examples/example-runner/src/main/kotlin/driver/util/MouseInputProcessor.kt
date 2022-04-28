package driver.util

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport

class MouseInputProcessor(val viewport: Viewport)
  : InputProcessor {

  val onLeftClick = mutableListOf<(worldMousePos: Vector2) -> Unit>()
  val onRightClick = mutableListOf<(worldMousePos: Vector2) -> Unit>()
  val onClick = mutableListOf<(worldMousePos: Vector2) -> Unit>()

  val onLeftDrag = mutableListOf<(worldMousePos: Vector2, dragDelta: Vector2) -> Unit>()
  val onRightDrag = mutableListOf<(worldMousePos: Vector2, dragDelta: Vector2) -> Unit>()
  val onDrag = mutableListOf<(worldMousePos: Vector2, dragDelta: Vector2) -> Unit>()

  val onScroll = mutableListOf<(amount: Float) -> Unit>()

  val worldMousePos = Vector2()
  val work = Vector2()

  var leftDown = false
    private set
  var rightDown = false
    private set
  var mouseCancelled = false
    private set
  var isDragging = false
    private set

  fun resetForFocus() {
    leftDown = false
    rightDown = false
    mouseCancelled = false
    isDragging = false
  }


  override fun keyDown(keycode: Int): Boolean {
    return false
  }

  override fun keyUp(keycode: Int): Boolean {
    return false
  }

  override fun keyTyped(character: Char): Boolean {
    return false
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (Input.Buttons.LEFT == button)
      leftDown = true
    if (Input.Buttons.RIGHT == button) {
      rightDown = true
    }

    if (leftDown && rightDown) {
      mouseCancelled = true
    }
    if (mouseCancelled)
      return true

    worldMousePos.set(screenX.toFloat(), screenY.toFloat())
    viewport.unproject(worldMousePos)
    isDragging = false
    return true
  }

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (!leftDown && !rightDown)
      return false
    if (Input.Buttons.LEFT == button)
      leftDown = false
    if (Input.Buttons.RIGHT == button)
      rightDown = false
    if (mouseCancelled && !leftDown && !rightDown) {
      mouseCancelled = false
      isDragging = false
      return true
    }
    if (mouseCancelled)
      return false

    when {
      !isDragging && Input.Buttons.LEFT == button -> {
        onClick.forEach { it(worldMousePos) }
        onLeftClick.forEach { it(worldMousePos) }
      }
      !isDragging && Input.Buttons.RIGHT == button -> {
        onClick.forEach { it(worldMousePos) }
        onRightClick.forEach { it(worldMousePos) }
      }
    }
    isDragging = false
    return true
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    if (mouseCancelled)
      return true
    if (!leftDown && !rightDown)
      return false

    // figure out the world position delta
    work.set(screenX.toFloat(), screenY.toFloat())
    viewport.unproject(work)
    work.set(worldMousePos.x - work.x, worldMousePos.y - work.y)
    isDragging = true

    when {
      rightDown -> onRightDrag.forEach { it(worldMousePos, work) }
      leftDown -> onLeftDrag.forEach { it(worldMousePos, work) }
    }
    onDrag.forEach { it(worldMousePos, work) }

    return true
  }


  override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
    worldMousePos.set(screenX.toFloat(), screenY.toFloat())
    viewport.unproject(worldMousePos)
    return false
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    onScroll.forEach { it(amountY) }
    return true
  }
}
