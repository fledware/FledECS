package fledware.ecs

data class EngineOptions(
    /**
     * automatically add newly created worlds to be updated.
     */
    val autoWorldUpdateOnCreate: Boolean = true
)