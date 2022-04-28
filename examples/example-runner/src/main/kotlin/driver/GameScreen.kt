package driver

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.style.label
import ktx.style.skin

abstract class GameScreen : ScreenAdapter() {
  val engine by lazy { runner.engine }
  val shapeRenderer by lazy { runner.shapeRenderer }
  val spriteBatch by lazy { runner.spriteBatch }
  val assetManager by lazy { runner.assetManager }
  val viewport = ExtendViewport(2000f, 0f, 2000f, 10_000f)
  val stage = Stage(ExtendViewport(2000f, 0f, 2000f, 10_000f), spriteBatch)
  val camera = (viewport.camera as OrthographicCamera).also {
    it.position.set(0f, 0f, 0f)
  }
  val fontHeader: BitmapFont
  val fontSmall: BitmapFont
  val fontTiny: BitmapFont

  init {
    val fontGenerator = FreeTypeFontGenerator(Gdx.files.classpath("kenney-future-narrow.ttf"))
    fontHeader = fontGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().also {
      it.size = 100
      it.borderColor = Color.BLACK
      it.borderWidth = 2f
    })
    fontSmall = fontGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().also {
      it.size = 75
      it.borderColor = Color.BLACK
      it.borderWidth = 2f
    })
    fontTiny = fontGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().also {
      it.size = 20
      it.borderColor = Color.BLACK
      it.borderWidth = 2f
    })
    fontGenerator.dispose()
  }

  val defaultSkin = skin {
    label("header") {
      font = fontHeader
      fontColor = Color.WHITE
    }
    label("small") {
      font = fontSmall
      fontColor = Color.WHITE
    }
    label("tiny") {
      font = fontTiny
      fontColor = Color.WHITE
    }
  }
}