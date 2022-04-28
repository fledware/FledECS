package fledware.ecs.benchmark


enum class ComponentType {
  POSITION, MOVEMENT, RADIUS, STATE
}

object Constants {
  const val DELTA_TIME = 1.0f / 60.0f
  const val BENCHMARK_ROUNDS = 10
  const val ENTITIES_SMALL_TEST = 10000
  const val ENTITIES_MEDIUM_TEST = 20000
  const val ENTITIES_BIG_TEST = 50000
  const val MIN_RADIUS = 0.1f
  const val MAX_RADIUS = 10.0f
  const val MIN_POS = -10f
  const val MAX_POS = 10.0f
  const val MIN_VEL = -1.0f
  const val MAX_VEL = 1.0f
  const val MIN_ACC = -0.1f
  const val MAX_ACC = 0.1f
  const val FRAMES_PER_REMOVAL = 10
  fun shouldHaveComponent(type: ComponentType, index: Int): Boolean {
    return when (type) {
      ComponentType.MOVEMENT -> index % 4 == 0
      ComponentType.RADIUS -> index % 3 == 0
      ComponentType.STATE -> index % 2 == 0
      ComponentType.POSITION -> true
    }
  }
}
