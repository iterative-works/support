# Module Migration Analysis - IW Support

## Current Migration Status

### Already Migrated
1. **core** (cross-platform: JS/JVM) - Base module with no dependencies
2. **entity** (cross-platform: JS/JVM) - Depends only on core

## Module Dependency Analysis

### Modules with Simple Dependencies (Good Candidates)

#### 1. **service-specs** (Priority: HIGH)
- **Type**: Cross-platform (JS/JVM)
- **Dependencies**: core only
- **Why migrate**: Simple dependency, cross-platform like core/entity
- **Complexity**: Low

#### 2. **tapir-support** (Priority: HIGH)
- **Type**: Cross-platform (JS/JVM)
- **Dependencies**: core only
- **Why migrate**: Many modules depend on it (ui, codecs, hashicorp, files-rest, autocomplete, http, paygate)
- **Complexity**: Low
- **Note**: Migrating this unlocks many other modules

#### 3. **mongo-support** (Priority: MEDIUM)
- **Type**: JVM-only
- **Dependencies**: core.jvm only
- **Why migrate**: Simple JVM module, good practice for JVM-only migrations
- **Complexity**: Low

#### 4. **sqldb-support** (Priority: MEDIUM)
- **Type**: JVM-only
- **Dependencies**: core.jvm only
- **Why migrate**: Similar to mongo-support
- **Complexity**: Low

#### 5. **email** (Priority: MEDIUM)
- **Type**: JVM-only
- **Dependencies**: core.jvm only
- **Why migrate**: Simple JVM module
- **Complexity**: Low

#### 6. **akka-persistence-support** (Priority: MEDIUM)
- **Type**: JVM-only
- **Dependencies**: core.jvm, entity.jvm (both migrated)
- **Why migrate**: All dependencies are ready
- **Complexity**: Low

### Modules with Complex Dependencies (Later Candidates)

#### 7. **files-core** (Priority: MEDIUM)
- **Type**: Cross-platform (JS/JVM)
- **Dependencies**: core only
- **Why migrate**: Enables files ecosystem migration
- **Complexity**: Low, but part of larger ecosystem

#### 8. **ui** (Priority: LOW)
- **Type**: Cross-platform (JS/JVM)
- **Dependencies**: core, tapir-support
- **Why migrate**: After tapir-support is ready
- **Complexity**: Medium

#### 9. **codecs** (Priority: LOW)
- **Type**: Cross-platform (Pure)
- **Dependencies**: core, entity, tapir-support
- **Why migrate**: After tapir-support is ready
- **Complexity**: Medium

#### 10. **hashicorp** (Priority: LOW)
- **Type**: Cross-platform (JS/JVM)
- **Dependencies**: core, service-specs, tapir-support
- **Why migrate**: After service-specs and tapir-support
- **Complexity**: Medium

## Recommended Migration Order

### Phase 1: Foundation Modules
1. **service-specs** - Simple, cross-platform, only depends on core
2. **tapir-support** - Simple, cross-platform, unlocks many modules

### Phase 2: JVM-only Modules
3. **mongo-support** - Good practice for JVM-only modules
4. **sqldb-support** - Similar structure to mongo
5. **email** - Another simple JVM module
6. **akka-persistence-support** - Uses both core and entity

### Phase 3: Dependent Modules
7. **files-core** - Foundation for files ecosystem
8. **ui** - Depends on tapir-support
9. **codecs** - Depends on multiple modules
10. **hashicorp** - Depends on multiple modules

### Phase 4: Complex Ecosystems
- files-rest, files-mongo, files-ui
- ui-forms, ui-core, ui-scalatags
- forms, forms-core, forms-http
- autocomplete
- paygate
- http

## Key Considerations

1. **Cross-platform modules** should maintain the same structure as core/entity
2. **JVM-only modules** can use a simpler structure without the js/jvm split
3. **Test migration** - Ensure test structure matches the new module structure
4. **Dependency management** - Use IWMillDeps for consistent dependency versions
5. **Publishing** - Ensure artifact names and organization match existing structure

## Next Steps

Start with **service-specs** and **tapir-support** as they:
- Have simple dependencies (only core)
- Are cross-platform like the already migrated modules
- Will unlock many other modules for migration
- Follow the same pattern as core/entity