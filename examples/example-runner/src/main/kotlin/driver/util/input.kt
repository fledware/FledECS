package driver.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

// ==================================================================
//
// input helpers
//
// ==================================================================

fun isKeyPressed(key1: Int) =
    Gdx.input.isKeyPressed(key1)

fun isKeyPressed(key1: Int, key2: Int) =
    Gdx.input.isKeyPressed(key1)
        || Gdx.input.isKeyPressed(key2)

fun isKeyPressed(key1: Int, key2: Int, key3: Int) =
    Gdx.input.isKeyPressed(key1)
        || Gdx.input.isKeyPressed(key2)
        || Gdx.input.isKeyPressed(key3)


fun isKeyJustPressed(key1: Int) =
    Gdx.input.isKeyJustPressed(key1)

fun isKeyJustPressed(key1: Int, key2: Int) =
    Gdx.input.isKeyJustPressed(key1)
        || Gdx.input.isKeyJustPressed(key2)

fun isKeyJustPressed(key1: Int, key2: Int, key3: Int) =
    Gdx.input.isKeyJustPressed(key1)
        || Gdx.input.isKeyJustPressed(key2)
        || Gdx.input.isKeyJustPressed(key3)

val isShiftPressed: Boolean
  get() = isKeyPressed(Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT)

val isAltPressed: Boolean
  get() = isKeyPressed(Input.Keys.ALT_LEFT, Input.Keys.ALT_RIGHT)

val isControlPressed: Boolean
  get() = isKeyPressed(Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT)
