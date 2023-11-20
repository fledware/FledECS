package fledware.ecs.updateengine

import fledware.ecs.update.DefaultUpdateEngine
import fledware.ecs.update.DefaultUpdateJob
import fledware.ecs.update.ForkJoinPoolUpdateEngine
import fledware.ecs.update.UpdateEngine
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

private class TestUpdateJob(key: Any, order: Int) : DefaultUpdateJob(key, order) {
  @Volatile
  var invokeCount = 0

  override fun update() {
    invokeCount++
  }

  override fun onRemoved() {
  }
}

class UpdateEngineTest {
  companion object {
    @JvmStatic
    fun data(): Stream<Arguments> = Stream.of(
        Arguments.of({ ForkJoinPoolUpdateEngine() }),
        Arguments.of({ DefaultUpdateEngine() }),
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  @Timeout(2)
  fun doesntThrowIfEmpty(factory: () -> UpdateEngine) {
    val engine = factory()
    engine.update()
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  @Timeout(2)
  fun canUpdateSingleGroupAndTask(factory: () -> UpdateEngine) {
    val engine = factory()
    val group = engine.createUpdateGroup("hello", 1)
    val job = TestUpdateJob("world", 1)
    group.registerJob(job)

    assertEquals(0, job.invokeCount)
    engine.update()
    assertEquals(1, job.invokeCount)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  @Timeout(2)
  fun canUpdateLotsOfGroupsQuickly(factory: () -> UpdateEngine) {
    val engine = factory()
    val jobs = mutableListOf<TestUpdateJob>()
    repeat(100) { groupIndex ->
      val group = engine.createUpdateGroup("hello-$groupIndex", groupIndex % 50)
      repeat(1) { jobIndex ->
        val job = TestUpdateJob("world-$jobIndex", jobIndex)
        group.registerJob(job)
        jobs += job
      }
    }

    repeat(100) { update ->
      println(update)
      jobs.forEach { job -> assertEquals(update, job.invokeCount) }
      engine.update()
      jobs.forEach { job -> assertEquals(update + 1, job.invokeCount) }
    }
  }
}