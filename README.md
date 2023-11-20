# FledECS

### Docs TODO
* entity sharing vs entity sharding
* system ordering
* world update grouping
* benchmarks
* updates
  * don't do updates in events

### API TODOs
* make lambdas not T.() -> Unit... do (thing: T) -> Unit
  * when chaining things together, it makes the "this" operator confusing
* make events registry return a cancelable
* custom events
