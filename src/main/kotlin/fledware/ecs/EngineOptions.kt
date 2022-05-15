package fledware.ecs

data class EngineOptions(
    /**
     * Automatically add newly created worlds to be updated.
     */
    val autoWorldUpdateOnCreate: Boolean = true,
    /**
     * Add checks to events to ensure ownership of entities.
     */
    val paranoidWorldEvents: Boolean = true,
    /**
     *
     */
    val defaultUpdateGroupName: String = "default",
    /**
     *
     */
    val defaultUpdateGroupOrder: Int = 0
)