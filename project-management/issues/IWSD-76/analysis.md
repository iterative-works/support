# Analysis: SQL-Based MessageCatalogue Implementation

**Issue:** IWSD-76
**Date:** 2025-11-10
**Prepared by:** Claude & Michal Příhoda

---

## Executive Summary

> **Key Point:** This is about **adding an alternative**, not replacing the current implementation.

This document proposes adding a SQL-based MessageCatalogue implementation using the Magnum library, to complement the existing JSON file-based approach. Both implementations will coexist, allowing projects to choose the approach that best fits their operational needs.

### Two Approaches, Different Use Cases

**JSON Files (Current - Remains Default):**
- Messages version-controlled in Git alongside code
- Deployed with the application
- Managed by developers through code review process
- Best for: messages tightly coupled to features/releases

**SQL Database (New Alternative):**
- Messages stored in database, editable at runtime
- Independent of code deployments
- Can be managed by non-developers through admin UI
- Best for: dynamic content, frequent updates, audit requirements

### What This Proposal Delivers

1. **SQL implementation** of the `MessageCatalogue` trait using Magnum
2. **Pre-load strategy** - all messages loaded at startup for fast, synchronous access
3. **Reload on demand** - explicit reload mechanism (no automatic polling)
4. **Migration tooling** to import messages from JSON to SQL
5. **Documentation** to help projects choose the right approach
6. **Both implementations maintained** - no deprecation of JSON approach

The proposed solution maintains the existing synchronous `MessageCatalogue` interface through pre-loading, provides a migration path for projects that choose SQL, and follows established Magnum patterns in the codebase.

### Key Design Decisions

- **Pre-load all messages** at startup into memory cache
- **Synchronous interface maintained** - no effects in `get()` methods
- **Reload on demand** via `reload(language: Option[Language])` method
- **Minimal repository** - only `getAllForLanguage()` and `bulkInsert()`
- **Language support** - Czech (CS) and English (EN)
- **Error handling** - fail fast on startup, return errors on reload

---

## Current State Analysis

### Implementation Overview

The MessageCatalogue system provides internationalization (i18n) support with the following architecture:

**Core Interface:** `MessageCatalogue` trait (shared across JVM/JS)
```scala
trait MessageCatalogue:
    def language: Language
    def get(id: MessageId): Option[String]
    def get(msg: UserMessage): Option[String]
    // ... fallback chains, nesting, formatting support
```

**Current JVM Implementation:** `InMemoryMessageCatalogue`
- Loads messages from JSON resource files at application startup
- File pattern: `/messages.json` (Czech), `/messages_en.json` (English)
- Stores in `Map[String, String]` for O(1) lookup
- No runtime updates possible

**Data Structure Example:**
```json
{
    "dokument.loading": "Nahrávám dokument '%s' ...",
    "dokument.nazev": "Název",
    "error.server": "Chyba serveru"
}
```

### JSON Approach: Strengths and Trade-offs

**Strengths:**
- **Version Control:** JSON files are in Git, providing full history and diff tracking
- **Deployment-time Consistency:** Messages are bundled with code, ensuring version alignment
- **Simple:** No database dependency, easy to understand and maintain
- **Fast:** In-memory lookup with zero network latency
- **Portable:** Works across JVM and JS platforms

**Trade-offs (where SQL alternative provides value):**
- **No Runtime Updates:** Messages are immutable after deployment; changes require redeployment
- **No Audit Trail:** Git shows file changes, but not who/when/why at the message level
- **No Dynamic Management:** No admin UI for non-developers to manage messages
- **Code Deployment Coupling:** Message changes require full deployment cycle
- **Multiple File Management:** Separate JSON files for each language need manual coordination

### Existing TODOs

The codebase already identifies several improvements needed:
- **Caching** (InMemoryMessageCatalogueService.scala:7)
- **Hierarchical JSON structure** (JsonMessageCatalogue.scala:10)
- **Generic message catalogue** (MessageCatalogue.scala:6-8)

---

## Requirements

### Functional Requirements

1. **Interface Compatibility:** Maintain existing synchronous `MessageCatalogue` trait - both implementations share the same interface
2. **Coexistence:** JSON and SQL implementations coexist; projects choose which to use
3. **Migration Path:** Provide tooling to migrate from JSON to SQL for projects that need it
4. **Language Support:** Support Czech (CS) and English (EN) - extensible to other languages
5. **Pre-load Strategy:** All messages loaded at startup for zero-latency runtime access
6. **Reload on Demand:** Explicit reload mechanism via `reload(language: Option[Language])` method
7. **Fallback Chains:** Support existing fallback mechanisms
8. **Prefix/Nesting:** Support hierarchical message organization
9. **Argument Formatting:** Support Java String.format() patterns

### Non-Functional Requirements

1. **Performance:** Sub-millisecond message retrieval (pre-loaded in-memory cache)
2. **Reliability:** Fail-fast on startup if messages cannot be loaded; reload returns errors but keeps existing cache
3. **Scalability:** Handle 10,000+ messages efficiently
4. **Maintainability:** Follow existing Magnum patterns in codebase
5. **Testability:** Full test coverage with TestContainers
6. **Security:** SQL injection protection via Magnum's parameterized queries

---

## Proposed Solution

### Database Schema

```sql
-- Core messages table
CREATE TABLE message_catalogue (
    id BIGSERIAL PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(5) NOT NULL,
    message_text TEXT NOT NULL,
    description TEXT,  -- Optional: describe message purpose
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_message_key_language UNIQUE (message_key, language)
);

-- Indexes for performance
CREATE INDEX idx_message_catalogue_key_lang ON message_catalogue(message_key, language);
CREATE INDEX idx_message_catalogue_language ON message_catalogue(language);
CREATE INDEX idx_message_catalogue_updated_at ON message_catalogue(updated_at);

-- Audit trail table (optional, for tracking changes)
CREATE TABLE message_catalogue_history (
    id BIGSERIAL PRIMARY KEY,
    message_catalogue_id BIGINT NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(5) NOT NULL,
    old_message_text TEXT,
    new_message_text TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(100),
    change_reason TEXT
);

CREATE INDEX idx_message_history_catalogue_id ON message_catalogue_history(message_catalogue_id);
CREATE INDEX idx_message_history_changed_at ON message_catalogue_history(changed_at);
```

### Magnum Domain Model

```scala
// File: core/jvm/src/main/scala/works/iterative/core/db/MessageCatalogueEntity.scala

package works.iterative.core.db

import com.augustnagro.magnum.*
import works.iterative.core.{Language, MessageId}
import java.time.Instant

@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class MessageCatalogueEntity(
    @Id id: Option[Long],
    messageKey: String,
    language: String,
    messageText: String,
    description: Option[String],
    createdAt: Instant,
    updatedAt: Instant,
    createdBy: Option[String],
    updatedBy: Option[String]
) derives DbCodec

object MessageCatalogueEntity:
    def fromMessage(
        key: MessageId,
        lang: Language,
        text: String,
        desc: Option[String] = None,
        user: Option[String] = None
    ): MessageCatalogueEntity =
        val now = Instant.now()
        MessageCatalogueEntity(
            id = None,
            messageKey = key,
            language = lang,
            messageText = text,
            description = desc,
            createdAt = now,
            updatedAt = now,
            createdBy = user,
            updatedBy = user
        )
```

### Repository Layer

Minimal interface focused on loading and migration. Admin operations (upsert/delete) can be added later when building admin UI.

```scala
// File: core/jvm/src/main/scala/works/iterative/core/repository/MessageCatalogueRepository.scala

package works.iterative.core.repository

import works.iterative.core.db.MessageCatalogueEntity
import works.iterative.core.Language
import zio.*

trait MessageCatalogueRepository:
    /** Retrieve all messages for a language - used for initial load and reload */
    def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueEntity]]

    /** Bulk insert messages - used for migration from JSON */
    def bulkInsert(entities: Seq[MessageCatalogueEntity]): Task[Unit]

object MessageCatalogueRepository:
    val layer: URLayer[Transactor, MessageCatalogueRepository] =
        ZLayer.fromFunction(MessageCatalogueRepositoryImpl.apply)
```

### Repository Implementation

```scala
// File: core/jvm/src/main/scala/works/iterative/core/repository/impl/MessageCatalogueRepositoryImpl.scala

package works.iterative.core.repository.impl

import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.*
import works.iterative.core.db.MessageCatalogueEntity
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.core.Language
import zio.*

class MessageCatalogueRepositoryImpl(transactor: Transactor)
    extends MessageCatalogueRepository:

    private val repo = Repo[MessageCatalogueEntity, MessageCatalogueEntity, Long]

    override def getAllForLanguage(language: Language): Task[Seq[MessageCatalogueEntity]] =
        transactor.connect:
            sql"""
                SELECT * FROM message_catalogue
                WHERE language = $language
            """.query[MessageCatalogueEntity].run()

    override def bulkInsert(entities: Seq[MessageCatalogueEntity]): Task[Unit] =
        transactor.transact:
            repo.insertAll(entities)
```

**Note on SQL Safety:** Magnum's `sql` string interpolation uses parameterized queries (prepared statements), providing protection against SQL injection attacks.

### SQL MessageCatalogue Implementation

Simple pre-loaded implementation - all messages loaded at startup, stored in memory for fast synchronous access.

```scala
// File: core/jvm/src/main/scala/works/iterative/core/service/impl/SqlMessageCatalogue.scala

package works.iterative.core.service.impl

import works.iterative.core.*
import zio.*

/** SQL-backed MessageCatalogue with pre-loaded messages */
class SqlMessageCatalogue(
    override val language: Language,
    messages: Map[String, String]  // Pre-loaded at startup
) extends MessageCatalogue:

    override def get(id: MessageId): Option[String] =
        messages.get(id)

    override def get(msg: UserMessage): Option[String] =
        get(msg.id).map: template =>
            try
                template.formatted(msg.args.map(_.toString)*)
            catch
                case e: Exception =>
                    s"[Formatting error for '${msg.id}': ${e.getMessage}]"

    override def root: MessageCatalogue = this

    override def nested(prefixes: String*): MessageCatalogue =
        NestedMessageCatalogue(this, currentPrefixes ++ prefixes)
```

**Key Points:**
- No effects in `get()` methods - pure Map lookup
- No `Unsafe.unsafe` needed - truly synchronous
- Same performance characteristics as JSON implementation
- Messages pre-loaded during layer construction

### Service Layer

```scala
// File: core/jvm/src/main/scala/works/iterative/core/service/impl/SqlMessageCatalogueService.scala

package works.iterative.core.service.impl

import works.iterative.core.*
import works.iterative.core.repository.MessageCatalogueRepository
import works.iterative.core.service.MessageCatalogueService
import zio.*

class SqlMessageCatalogueService(
    repository: MessageCatalogueRepository,
    cacheRef: Ref[Map[Language, Map[String, String]]],
    defaultLanguage: Language = Language.CS
) extends MessageCatalogueService:

    override def messages: UIO[MessageCatalogue] =
        forLanguage(defaultLanguage)

    override def forLanguage(language: Language): UIO[MessageCatalogue] =
        cacheRef.get.map { cache =>
            val messages = cache.getOrElse(language, Map.empty)
            SqlMessageCatalogue(language, messages)
        }

    /** Reload messages from database on demand */
    def reload(language: Option[Language] = None): Task[Unit] =
        language match
            case Some(lang) =>
                // Reload single language
                for
                    entities <- repository.getAllForLanguage(lang)
                    messageMap = entities.map(e => e.messageKey -> e.messageText).toMap
                    _ <- cacheRef.update(_.updated(lang, messageMap))
                    _ <- ZIO.logInfo(s"Reloaded $lang: ${entities.size} messages")
                yield ()

            case None =>
                // Reload all configured languages
                for
                    languages <- cacheRef.get.map(_.keys.toSeq)
                    newCache <- ZIO.foreach(languages) { lang =>
                        repository.getAllForLanguage(lang).map { entities =>
                            lang -> entities.map(e => e.messageKey -> e.messageText).toMap
                        }
                    }.map(_.toMap)
                    _ <- cacheRef.set(newCache)
                    _ <- ZIO.logInfo(s"Reloaded all languages: ${languages.mkString(", ")}")
                yield ()

object SqlMessageCatalogueService:
    /** Create service with pre-loaded messages for specified languages */
    def make(
        repository: MessageCatalogueRepository,
        languages: Seq[Language],
        defaultLanguage: Language = Language.CS
    ): Task[SqlMessageCatalogueService] =
        for
            // Pre-load all messages at startup
            initialCache <- ZIO.foreach(languages) { lang =>
                repository.getAllForLanguage(lang).map { entities =>
                    lang -> entities.map(e => e.messageKey -> e.messageText).toMap
                }
            }.map(_.toMap)

            cacheRef <- Ref.make(initialCache)
            _ <- ZIO.logInfo(s"Loaded messages for languages: ${languages.mkString(", ")}")
        yield SqlMessageCatalogueService(repository, cacheRef, defaultLanguage)

    /** ZIO Layer - fails if messages cannot be loaded */
    def layer(
        languages: Seq[Language] = Seq(Language.CS, Language.EN),
        defaultLanguage: Language = Language.CS
    ): URLayer[MessageCatalogueRepository, MessageCatalogueService] =
        ZLayer.fromZIO:
            for
                repo <- ZIO.service[MessageCatalogueRepository]
                service <- make(repo, languages, defaultLanguage).orDie
            yield service
```

**Key Design Points:**
- **Pre-load at startup:** All messages loaded during layer construction
- **Fail-fast:** If messages can't be loaded, application fails to start (`.orDie`)
- **Reload on demand:** `reload(language)` can be called explicitly (e.g., from admin UI)
- **Error handling for reload:** Returns `Task[Unit]` - errors propagated to caller, existing cache unchanged
- **Configurable languages:** Specify which languages to load at startup

### Flyway Migration

```sql
-- File: core/jvm/src/main/resources/db/migration/V3__create_message_catalogue.sql

-- Core messages table
CREATE TABLE message_catalogue (
    id BIGSERIAL PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(5) NOT NULL,
    message_text TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uq_message_key_language UNIQUE (message_key, language)
);

-- Indexes for performance
CREATE INDEX idx_message_catalogue_key_lang ON message_catalogue(message_key, language);
CREATE INDEX idx_message_catalogue_language ON message_catalogue(language);
CREATE INDEX idx_message_catalogue_updated_at ON message_catalogue(updated_at);

-- Audit trail table
CREATE TABLE message_catalogue_history (
    id BIGSERIAL PRIMARY KEY,
    message_catalogue_id BIGINT NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    language VARCHAR(5) NOT NULL,
    old_message_text TEXT,
    new_message_text TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by VARCHAR(100),
    change_reason TEXT
);

CREATE INDEX idx_message_history_catalogue_id ON message_catalogue_history(message_catalogue_id);
CREATE INDEX idx_message_history_changed_at ON message_catalogue_history(changed_at);

-- Trigger for audit trail
CREATE OR REPLACE FUNCTION message_catalogue_audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        INSERT INTO message_catalogue_history (
            message_catalogue_id, message_key, language,
            old_message_text, new_message_text, changed_by
        ) VALUES (
            OLD.id, OLD.message_key, OLD.language,
            OLD.message_text, NEW.message_text, NEW.updated_by
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER message_catalogue_audit
    AFTER UPDATE ON message_catalogue
    FOR EACH ROW
    WHEN (OLD.message_text IS DISTINCT FROM NEW.message_text)
    EXECUTE FUNCTION message_catalogue_audit_trigger();
```

---

## Migration Strategy

### Phase 1: Infrastructure Setup

1. **Create Flyway Migration:**
   - Add `V3__create_message_catalogue.sql` to `db/migration/`
   - Run migration to create tables

2. **Implement Repository Layer:**
   - Create `MessageCatalogueEntity` domain model
   - Implement `MessageCatalogueRepository` trait
   - Implement `MessageCatalogueRepositoryImpl`
   - Add comprehensive unit tests

### Phase 2: Data Migration

Create a migration script to import existing JSON messages:

```scala
// File: core/jvm/src/main/scala/works/iterative/core/migration/MessageCatalogueMigration.scala

object MessageCatalogueMigration:
    def migrateFromJson(
        repository: MessageCatalogueRepository,
        language: Language,
        jsonResourcePath: String
    ): Task[Unit] =
        for
            // Load existing JSON
            json <- ZIO.attempt(loadJsonResource(jsonResourcePath))
            messages <- ZIO.fromEither(json.as[Map[String, String]])

            // Convert to entities
            entities = messages.map: (key, text) =>
                MessageCatalogueEntity.fromMessage(
                    MessageId(key),
                    language,
                    text,
                    desc = Some(s"Migrated from $jsonResourcePath")
                )

            // Bulk insert
            _ <- repository.bulkInsert(entities.toSeq)
            _ <- ZIO.logInfo(s"Migrated ${entities.size} messages for $language")
        yield ()
```

### Phase 3: Service Implementation

1. **Implement SqlMessageCatalogue:**
   - Create cached implementation
   - Implement cache invalidation
   - Add monitoring/metrics

2. **Implement SqlMessageCatalogueService:**
   - Wire up ZIO layers
   - Configure default language

### Phase 4: Testing

1. **Unit Tests:**
   - Repository CRUD operations
   - Cache behavior
   - Fallback chains
   - Formatting

2. **Integration Tests:**
   - Full database round-trips
   - Cache invalidation
   - Concurrent access
   - Migration scripts

### Phase 5: Adoption Strategy

The SQL implementation is provided as an **alternative**, not a replacement:

1. **Configuration-Based Selection:** Projects configure which implementation to use via application config
2. **Documentation:** Document use cases for each approach to guide project teams
3. **Migration Support:** Provide migration tooling and guidance for projects choosing to move to SQL
4. **Coexistence:** Both implementations remain available; JSON implementation is not deprecated

**Recommended approach for projects:**
- **Stay with JSON:** If messages are tightly coupled to code releases, managed by developers, and don't need runtime updates
- **Migrate to SQL:** If messages need runtime updates, non-developer management, audit trails, or are managed independently from deployments

---

## Performance Considerations

### Caching Strategy

**Pre-loaded In-Memory Cache:**

- All messages loaded at startup into `Ref[Map[Language, Map[String, String]]]`
- Zero-latency runtime lookups (pure Map access)
- No automatic background refresh
- Explicit reload via `reload(language: Option[Language])` method
- Database queries only during:
  - Application startup (pre-load)
  - Explicit reload requests
  - Migration operations

### Expected Performance

| Operation | Performance | Notes |
|-----------|-------------|-------|
| Message lookup (`get()`) | ~0.001ms | Pure in-memory Map lookup |
| Initial load at startup | ~50-100ms per language | One-time cost |
| Reload single language | ~50-100ms | On-demand only |
| Reload all languages | ~100-200ms | On-demand only |

### Scalability Analysis

**JSON Approach:**
- Memory: ~1MB per 1000 messages
- Startup time: ~50ms per language
- Runtime updates: Not possible
- Lookup: Pure in-memory (~0.001ms)

**SQL Approach (with pre-load):**
- Memory: ~1MB per language (same as JSON) + database storage
- Startup time: ~100ms per language (includes database query)
- Runtime updates: Via explicit reload (~50-100ms)
- Lookup: Pure in-memory (~0.001ms)
- Scales to 10,000+ messages per language without issue

**Memory Estimate:**
- 1,000 messages × 100 chars average = ~100KB per language
- 10,000 messages = ~1MB per language
- 2 languages (CS + EN) = ~2MB total (negligible)

**Startup Time:**
- SQL query: ~20-30ms per language
- Deserialization: ~20-30ms per language
- Total per language: ~50-100ms
- Parallel loading: Could load languages concurrently if needed

---

## Testing Strategy

### Unit Tests

```scala
class MessageCatalogueRepositorySpec extends ZIOSpecDefault:
    def spec = suite("MessageCatalogueRepository")(
        test("bulk insert and retrieve messages"):
            for
                repo <- ZIO.service[MessageCatalogueRepository]
                entities = Seq(
                    MessageCatalogueEntity.fromMessage(
                        MessageId("test.key1"),
                        Language.EN,
                        "Test message 1"
                    ),
                    MessageCatalogueEntity.fromMessage(
                        MessageId("test.key2"),
                        Language.EN,
                        "Test message 2"
                    )
                )
                _ <- repo.bulkInsert(entities)
                retrieved <- repo.getAllForLanguage(Language.EN)
            yield assertTrue(
                retrieved.size == 2,
                retrieved.exists(_.messageKey == "test.key1")
            )
        ,
        test("get all for language returns empty for unknown language"):
            for
                repo <- ZIO.service[MessageCatalogueRepository]
                messages <- repo.getAllForLanguage(Language.CS)
            yield assertTrue(messages.isEmpty)
    ).provide(
        PostgreSQLTestingLayers.testDbLayer,
        MessageCatalogueRepository.layer
    )
```

### Integration Tests

```scala
class SqlMessageCatalogueServiceSpec extends ZIOSpecDefault:
    def spec = suite("SqlMessageCatalogueService")(
        test("pre-load messages at startup"):
            for
                repo <- ZIO.service[MessageCatalogueRepository]
                // Insert test messages
                _ <- repo.bulkInsert(Seq(
                    MessageCatalogueEntity.fromMessage(
                        MessageId("greeting"),
                        Language.EN,
                        "Hello"
                    )
                ))
                // Create service (pre-loads messages)
                service <- SqlMessageCatalogueService.make(
                    repo,
                    Seq(Language.EN)
                )
                catalogue <- service.forLanguage(Language.EN)
                message = catalogue("greeting")
            yield assertTrue(message == "Hello")
        ,
        test("reload updates messages"):
            for
                repo <- ZIO.service[MessageCatalogueRepository]
                service <- SqlMessageCatalogueService.make(
                    repo,
                    Seq(Language.EN)
                )

                // Initial message
                catalogue1 <- service.forLanguage(Language.EN)
                msg1 = catalogue1.get(MessageId("test"))

                // Update in database
                _ <- repo.bulkInsert(Seq(
                    MessageCatalogueEntity.fromMessage(
                        MessageId("test"),
                        Language.EN,
                        "Updated"
                    )
                ))

                // Reload
                _ <- service.reload(Some(Language.EN))
                catalogue2 <- service.forLanguage(Language.EN)
                msg2 = catalogue2.get(MessageId("test"))
            yield assertTrue(
                msg1.isEmpty,
                msg2.contains("Updated")
            )
    ).provide(
        PostgreSQLTestingLayers.testDbLayer,
        MessageCatalogueRepository.layer
    )
```

---

## Implementation Checklist

### Database Layer
- [ ] Create Flyway migration `V3__create_message_catalogue.sql`
- [ ] Create `MessageCatalogueEntity` case class with `@Table` annotation
- [ ] Implement `MessageCatalogueRepository` trait (2 methods: `getAllForLanguage`, `bulkInsert`)
- [ ] Implement `MessageCatalogueRepositoryImpl` with Magnum
- [ ] Write unit tests for repository (with TestContainers)

### Service Layer
- [ ] Implement `SqlMessageCatalogue` (simple pre-loaded implementation)
- [ ] Implement `SqlMessageCatalogueService` with pre-load and reload logic
- [ ] Create ZIO layer with language configuration (CS, EN)
- [ ] Write integration tests for pre-load and reload

### Migration
- [ ] Create JSON-to-SQL migration script
- [ ] Test migration with existing messages.json files
- [ ] Document migration process

### Configuration
- [ ] Database connection already configured (reuse existing PostgreSQL setup)
- [ ] Language configuration in layer construction (Seq(Language.CS, Language.EN))

### Documentation
- [ ] Update MessageCatalogueService documentation
- [ ] Document reload mechanism for admin UI integration
- [ ] Create project selection guide (JSON vs SQL decision criteria)

### Production Readiness
- [ ] Add logging for startup message loading
- [ ] Add logging for reload operations
- [ ] Add error handling documentation
- [ ] Test startup failure scenarios (database unavailable)

---

## Risk Assessment

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Cache inconsistency | Medium | Low | Timestamp-based invalidation + polling |
| Database connection failure | High | Low | Connection pooling + retry logic |
| Migration data loss | High | Very Low | Dry-run testing + backups |
| Performance degradation | Medium | Low | Load testing + monitoring |
| Backward compatibility | Medium | Low | Maintain interface + feature flag |

### Operational Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Deployment complexity | Low | Medium | Gradual rollout + feature flags |
| Message editing errors | Medium | Medium | Audit trail + rollback capability |
| Cache memory usage | Low | Low | LRU eviction + capacity limits |

---

## Implementation Approaches Comparison

### 1. JSON Files (Current - Remains Available)
**Use Cases:** Messages tied to code releases, developer-managed, deployment-time updates
**Pros:**
- Simple, no database dependency
- Git version control with full history
- Bundled with application, version-aligned
- Zero network latency

**Cons:**
- No runtime updates
- Requires deployment for message changes
- No admin UI for non-developers

**Status:** **Remains the default and recommended for most use cases**

### 2. SQL Database (Proposed Addition)
**Use Cases:** Runtime-editable messages, non-developer management, dynamic content, audit requirements
**Pros:**
- Runtime updates without deployment
- Audit trail (who/when/why)
- Admin UI potential
- Independent from code releases

**Cons:**
- Database dependency
- Caching complexity
- Additional infrastructure

**Status:** **Added as alternative for specific use cases**

### 3. NoSQL (e.g., Redis) - Not Recommended
**Pros:** Very fast, built-in TTL
**Cons:** New technology stack, no relational features, no SQL audit capabilities

### 4. External Service (e.g., Phrase, Lokalise) - Not Recommended
**Pros:** Feature-rich, managed service
**Cons:** External dependency, cost, vendor lock-in, network dependency

### 5. Hybrid Approach - Not Recommended
**Pros:** Flexibility
**Cons:** Two sources of truth, complexity, confusion about which to use

**Decision:** Provide both JSON and SQL implementations as alternatives, allowing projects to choose based on their needs. SQL with Magnum is chosen for the database option due to consistency with existing architecture and team familiarity.

---

## Timeline Estimate

| Phase | Effort | Duration |
|-------|--------|----------|
| Database schema + migration | 4 hours | 1 day |
| Repository implementation | 8 hours | 1 day |
| Service layer + caching | 8 hours | 1 day |
| Data migration script | 4 hours | 0.5 day |
| Unit + integration tests | 8 hours | 1 day |
| Production deployment | 4 hours | 0.5 day |
| **Total** | **36 hours** | **5 days** |

*Assumes single developer, includes testing and documentation.*

---

## Recommendations

1. **Implement SQL as an alternative** (not a replacement) following the phased approach
2. **Start with repository layer** to validate Magnum patterns
3. **Create migration tooling** to help projects transition from JSON to SQL if they choose
4. **Document decision criteria** to help projects choose between JSON and SQL approaches
5. **Keep JSON as default** - projects opt-in to SQL when they need runtime updates
6. **Monitor cache performance** for SQL implementation in production
7. **Consider admin UI** for SQL-based message management (future enhancement)
8. **Maintain both implementations** - no plans to deprecate JSON approach

**When to recommend SQL to projects:**
- Messages need frequent updates (not tied to code releases)
- Non-developers need to manage messages
- Audit trail is required
- Messages managed independently from deployments

**When to recommend staying with JSON:**
- Messages are tightly coupled to features/code
- Developers manage all message updates
- Messages deployed with application versions
- Simple setup preferred (no database dependency)

---

## Implementation Selection Guide

### For Project Teams: Choosing Your Approach

Both implementations share the `MessageCatalogue` interface, so the choice is primarily operational:

| Criteria | JSON Files | SQL Database |
|----------|-----------|--------------|
| **Message ownership** | Developers | Could be non-developers |
| **Update frequency** | With code releases | Independent of releases |
| **Version control** | Git (with code) | Database audit trail |
| **Update mechanism** | Deployment | Runtime (API/UI) |
| **Infrastructure** | None (bundled) | PostgreSQL database |
| **Startup time** | ~50ms | ~100ms (pre-load from DB) |
| **Runtime performance** | In-memory | In-memory (same) |
| **Audit trail** | Git history | Database history |
| **Complexity** | Low | Medium |

### Configuration Example

Projects configure which implementation to use:

```scala
// Using JSON (default)
val layer = InMemoryMessageCatalogueService.layer

// Using SQL (opt-in) - specify languages to pre-load
val layer =
    PostgreSQLDatabaseSupport.layer >+>
    MessageCatalogueRepository.layer >+>
    SqlMessageCatalogueService.layer(
        languages = Seq(Language.CS, Language.EN),
        defaultLanguage = Language.CS
    )
```

**Note:** With SQL implementation, messages are pre-loaded at startup for specified languages. Application will fail to start if messages cannot be loaded (fail-fast approach).

### Migration Path (JSON → SQL)

When a project decides to move from JSON to SQL:

1. Set up PostgreSQL database (if not already present)
2. Run Flyway migration to create tables
3. Use migration tool to import JSON messages to database
4. Update application configuration to use SQL implementation
5. Deploy and verify
6. Optionally remove JSON resource files

**Important:** This is a one-way migration per project. Choose based on long-term needs.

---

## Next Steps

1. **Review this analysis** with team for feedback
2. **Create implementation tasks** in Linear (subtasks of IWSD-76)
3. **Set up development environment** with local PostgreSQL
4. **Begin Phase 1**: Database schema and repository layer
5. **Schedule regular check-ins** during implementation

---

## References

### Codebase Files Analyzed

**MessageCatalogue Core:**
- `/core/shared/src/main/scala/works/iterative/core/MessageCatalogue.scala`
- `/core/shared/src/main/scala/works/iterative/core/MessageId.scala`
- `/core/shared/src/main/scala/works/iterative/core/UserMessage.scala`
- `/core/shared/src/main/scala/works/iterative/core/Language.scala`

**Current Implementation:**
- `/core/jvm/src/main/scala/works/iterative/core/service/impl/InMemoryMessageCatalogue.scala`
- `/core/jvm/src/main/scala/works/iterative/core/service/impl/InMemoryMessageCatalogueService.scala`

**Magnum Reference:**
- `/sqldb/src/main/scala/works/iterative/sqldb/PostgreSQLTransactor.scala`
- `/sqldb/src/main/scala/works/iterative/sqldb/PostgreSQLDatabaseSupport.scala`
- `/core/jvm/src/main/scala/works/iterative/core/repository/`

**Test Data:**
- `/ui/scenarios/src/main/static/resources/messages.json`

### External Resources

- Magnum documentation: https://github.com/AugustNagro/magnum
- ZIO Cache: https://zio.dev/reference/caching/
- Flyway migrations: https://flywaydb.org/

---

**End of Analysis**
