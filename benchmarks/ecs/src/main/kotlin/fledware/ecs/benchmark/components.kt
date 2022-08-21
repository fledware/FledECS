package fledware.ecs.benchmark

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class MovementComponent : Component {
	val velocity = Vector2()
	val accel = Vector2()
}

class PositionComponent : Component {
	val pos = Vector3()
	val scale = Vector2(1.0f, 1.0f)
	var rotation = 0.0f
}

class RadiusComponent : Component {
	var radius = 1.0f
}

class RemovalComponent : Component

class StateComponent : Component {
	private var state = 0
	var time = 0.0f
	fun get(): Int {
		return state
	}

	fun set(newState: Int) {
		state = newState
		time = 0.0f
	}
}
