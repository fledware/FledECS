package driver.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max
import kotlin.math.min

var defaultGridColor = Color(Color.GRAY).also { it.a = 0.5f }


fun screenMin(position: Float, viewport: Float, zoom: Float, size: Int, min: Int): Int {
  return max(((position - viewport / 2f * zoom) / size - 1).toInt() * size, min)
}

fun screenMax(position: Float, viewport: Float, zoom: Float, size: Int, max: Int): Int {
  return min(((position + viewport / 2f * zoom) / size + 1).toInt() * size, max)
}

fun ShapeRenderer.drawGrid(viewport: Viewport,
                           size: Int,
                           color: Color = defaultGridColor,
                           absoluteMinX: Int = Int.MIN_VALUE,
                           absoluteMinY: Int = Int.MIN_VALUE,
                           absoluteMaxX: Int = Int.MAX_VALUE,
                           absoluteMaxY: Int = Int.MAX_VALUE) {
  val camera = viewport.camera
  val zoom = (viewport.camera as? OrthographicCamera)?.zoom ?: 1f
  val xMin = screenMin(camera.position.x, viewport.worldWidth, zoom, size, absoluteMinX)
  val xMax = screenMax(camera.position.x, viewport.worldWidth, zoom, size, absoluteMaxX)
  val yMin = screenMin(camera.position.y, viewport.worldHeight, zoom, size, absoluteMinY)
  val yMax = screenMax(camera.position.y, viewport.worldHeight, zoom, size, absoluteMaxY)

  Gdx.gl.glLineWidth(0.5f)
  this.begin(ShapeRenderer.ShapeType.Line)
  this.color = color
  for (x in xMin..xMax step size) {
    this.line(x.toFloat(), yMin.toFloat(), x.toFloat(), yMax.toFloat())
  }
  for (y in yMin..yMax step size) {
    this.line(xMin.toFloat(), y.toFloat(), xMax.toFloat(), y.toFloat())
  }
  this.end()
}
