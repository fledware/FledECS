package fledware.ecs

interface EntityFactory {
  val engine: Engine
  fun importEntity(entity: Entity)
  fun importEntities(entities: List<Entity>) = entities.forEach { importEntity(it) }
  fun createEntity(decorator: Entity.() -> Unit): Entity
  fun createEntity(name: String, decorator: Entity.() -> Unit) = createEntity {
    this.name = name
    this.decorator()
  }
}
