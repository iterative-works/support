# Message Catalogue Implementation Guide

## Overview

This guide helps you choose between JSON and SQL implementations of the Message Catalogue system and provides configuration examples for each approach.

## When to Use JSON vs SQL

### Decision Criteria

| Criterion | JSON Implementation | SQL Implementation |
|-----------|-------------------|-------------------|
| **Use When** | Messages rarely change, simple deployment | Messages change frequently, admin UI needed |
| **Message Updates** | Requires code deployment | Update via database, reload via API |
| **Startup Time** | Instant (messages bundled with code) | ~100-200ms for 10K messages |
| **Runtime Performance** | O(1) map lookup | O(1) map lookup (pre-loaded cache) |
| **Memory Usage** | One copy per language | One copy per language (same as JSON) |
| **Deployment Complexity** | Low (static files in resources) | Medium (database schema + data migration) |
| **Hot Reload** | Not supported | Supported via `reload()` method |
| **Admin UI Support** | Not applicable | Full support for CRUD operations |
| **Version Control** | Messages tracked in git | Messages in database, schema in git |
| **Internationalization Workflow** | Translators work with JSON files | Translators use admin UI or database tools |
| **Testing** | Simple (mock with test messages) | Requires test database or mocking |
| **Best For** | Static websites, mobile apps, microservices | SaaS platforms, content management systems |

### Recommendation

- **Choose JSON** if your messages are stable, managed by developers, and deployed with code
- **Choose SQL** if your messages need frequent updates, non-technical editors, or admin UI management
- **Start with JSON** and migrate to SQL when you need runtime updates or admin UI

## Configuration Examples

### JSON Implementation (Frontend/ScalaJS)

```scala
import works.iterative.ui.JsonMessageCatalogue
import works.iterative.core.Language
import scala.scalajs.js

// Define messages as JavaScript dictionary
val englishMessages = js.Dictionary(
  "welcome" -> "Welcome, %s!",
  "error.notFound" -> "Item not found",
  "button.submit" -> "Submit"
)

// Create message catalogue
val catalogue = JsonMessageCatalogue(Language.EN, englishMessages)

// Use messages
val message = catalogue(UserMessage(MessageId("welcome"), "John"))
// Returns: "Welcome, John!"
```

### SQL Implementation (Backend/JVM)

#### Layer Composition

```scala
import works.iterative.core.Language
import works.iterative.core.service.MessageCatalogueService
import works.iterative.core.service.impl.SqlMessageCatalogueService
import works.iterative.sqldb.postgresql.PostgreSQLMessageCatalogueRepository
import zio.*

// Define supported languages
val supportedLanguages = Seq(Language.EN, Language.CS, Language.DE)
val defaultLanguage = Language.EN

// Create application layer with SQL message catalogue
val appLayer: ZLayer[Any, Throwable, MessageCatalogueService] =
  PostgreSQLDatabaseSupport.layer >>>
  PostgreSQLMessageCatalogueRepository.layer >>>
  SqlMessageCatalogueService.layer(supportedLanguages, defaultLanguage)

// Use in your application
val program = for {
  catalogue <- MessageCatalogueService.messages
  message = catalogue(MessageId("welcome"))
  _ <- Console.printLine(message)
} yield ()

program.provide(appLayer)
```

#### With Custom Database Configuration

```scala
import works.iterative.sqldb.postgresql.{DatabaseConfig, PostgreSQLDatabaseSupport}

val dbConfig = DatabaseConfig(
  host = "localhost",
  port = 5432,
  database = "myapp",
  user = "appuser",
  password = "secret"
)

val customLayer: ZLayer[Any, Throwable, MessageCatalogueService] =
  ZLayer.succeed(dbConfig) >>>
  PostgreSQLDatabaseSupport.layer >>>
  PostgreSQLMessageCatalogueRepository.layer >>>
  SqlMessageCatalogueService.layer(supportedLanguages, defaultLanguage)
```

## Migration Path: JSON → SQL

### Step 1: Prepare Database Schema

Run Flyway migration to create the `message_catalogue` table:

```bash
# PostgreSQL
mill core.jvm.runMain org.flywaydb.core.Flyway migrate

# MySQL/MariaDB
mill core.jvm.runMain org.flywaydb.core.Flyway migrate -configFiles=flyway-mysql.conf
```

This creates the table:

```sql
CREATE TABLE message_catalogue (
  message_key VARCHAR(255) NOT NULL,
  language_code VARCHAR(10) NOT NULL,
  message_text TEXT NOT NULL,
  description TEXT,
  last_modified TIMESTAMP,
  PRIMARY KEY (message_key, language_code)
);
```

### Step 2: Migrate JSON Data to Database

Use the migration CLI tool to import JSON files:

```bash
# For each language, run the migration CLI
mill core.jvm.runMain works.iterative.sqldb.postgresql.migration.MessageCatalogueMigrationCLI \
  --language=en \
  --resource=/messages_en.json

mill core.jvm.runMain works.iterative.sqldb.postgresql.migration.MessageCatalogueMigrationCLI \
  --language=cs \
  --resource=/messages_cs.json
```

The CLI tool:
- Loads JSON from resources
- Parses as `Map[String, String]`
- Bulk inserts to database
- Logs progress and completion

### Step 3: Update Application Configuration

Replace JSON layer with SQL layer in your application setup:

```scala
// Before (JSON)
val layer = JsonMessageCatalogueService.layer(defaultLanguage)

// After (SQL)
val layer =
  PostgreSQLDatabaseSupport.layer >>>
  PostgreSQLMessageCatalogueRepository.layer >>>
  SqlMessageCatalogueService.layer(
    languages = Seq(Language.EN, Language.CS),
    defaultLanguage = Language.EN
  )
```

### Step 4: Test Pre-load at Startup

Start your application and verify logs:

```
[info] Pre-loading message catalogues for 2 languages
[info] Loaded en: 1247 messages
[info] Loaded cs: 1198 messages
[info] Message catalogue service initialized: 2445 total messages
```

If startup fails, the application will not start (fail-fast behavior via `.orDie`).

### Step 5: Verify Message Retrieval

Test that messages are retrieved correctly:

```scala
val testProgram = for {
  catalogue <- MessageCatalogueService.messages
  welcome = catalogue(MessageId("welcome"))
  _ <- Console.printLine(s"Welcome message: $welcome")

  csCatalogue <- MessageCatalogueService.forLanguage(Language.CS)
  csWelcome = csCatalogue(MessageId("welcome"))
  _ <- Console.printLine(s"Czech welcome: $csWelcome")
} yield ()
```

### Step 6: Deploy to Production

Follow the deployment runbook (see `message-catalogue-deployment.md`).

### Rollback Plan

If issues occur, revert to JSON implementation:

1. Update configuration to use JSON layer
2. Restart application
3. Database remains unchanged (can retry migration later)

## Performance Characteristics

| Operation | JSON Implementation | SQL Implementation |
|-----------|-------------------|-------------------|
| **Application Startup** | ~0ms (no I/O) | ~100ms for 5K messages, ~200ms for 10K |
| **Message Lookup** | O(1) map lookup | O(1) map lookup (pre-loaded cache) |
| **Memory per Language** | ~500KB for 5K messages | ~500KB for 5K messages (same) |
| **Hot Reload** | Not supported | ~50-200ms depending on message count |
| **Concurrent Access** | Thread-safe (immutable) | Thread-safe (ZIO Ref atomic updates) |
| **Database Queries During Lookup** | N/A | Zero (all queries at startup/reload) |

### Performance Notes

- **SQL startup time is negligible** for most applications (<200ms even with 10K messages)
- **Runtime performance is identical** - both use in-memory Map lookups
- **Reload is fast** because it's a simple SELECT and cache update
- **No database queries during message retrieval** - SQL implementation is purely memory-based after pre-load

## Operational Considerations

### JSON Implementation

**Deployment:**
- Package JSON files with application JAR
- No external dependencies
- No database setup required

**Maintenance:**
- Update JSON files in source code
- Deploy new application version
- Cannot update messages without redeployment

**Monitoring:**
- No runtime monitoring needed
- Messages always available (bundled with code)

**Failure Modes:**
- Missing JSON file: Application startup fails
- Malformed JSON: Parsing error at startup
- Missing message key: Returns key as fallback

### SQL Implementation

**Deployment:**
- Run database schema migration (Flyway)
- Run data migration (CLI tool)
- Update application configuration
- Restart application

**Maintenance:**
- Update messages via admin UI or SQL
- Call `reload()` API to refresh cache
- No redeployment needed for message changes

**Monitoring:**
- Track startup time (should be <200ms for 10K messages)
- Monitor reload duration and frequency
- Alert on startup failures (messages not loaded)
- Log message counts per language

**Failure Modes:**
- Database unavailable at startup: Application fails to start (fail-fast)
- Database unavailable during reload: Error returned, existing cache unchanged
- Missing messages: Returns key as fallback (same as JSON)

## Layer Composition Examples

### Backend with SQL Messages

```scala
import works.iterative.core.Language
import works.iterative.core.service.MessageCatalogueService
import works.iterative.sqldb.postgresql.*
import zio.*

object MyApplication extends ZIOAppDefault {

  val languages = Seq(Language.EN, Language.CS, Language.DE)

  val appLayer =
    PostgreSQLDatabaseSupport.layer >>>
    PostgreSQLMessageCatalogueRepository.layer >>>
    SqlMessageCatalogueService.layer(languages, Language.EN)

  def run = myProgram.provide(appLayer)
}
```

### Backend with JSON Messages (Fallback)

```scala
import works.iterative.core.service.impl.JsonMessageCatalogueService
import works.iterative.core.Language

object MyApplication extends ZIOAppDefault {

  val appLayer = JsonMessageCatalogueService.layer(Language.EN)

  def run = myProgram.provide(appLayer)
}
```

### Frontend (ScalaJS) with JSON

```scala
import works.iterative.ui.JsonMessageCatalogue
import scala.scalajs.js

// Messages loaded from external JSON or embedded in JS bundle
val messages = js.Dictionary(
  "welcome" -> "Welcome!",
  "goodbye" -> "Goodbye!"
)

val catalogue = JsonMessageCatalogue(Language.EN, messages)
```

## Troubleshooting

### SQL Implementation Issues

#### Application fails to start with "Failed to load messages"

**Cause:** Database is unavailable or table doesn't exist.

**Solution:**
1. Verify database is running: `psql -h localhost -U user -d myapp`
2. Check Flyway migrations ran: `SELECT * FROM flyway_schema_history;`
3. Verify table exists: `\dt message_catalogue`
4. Check table has data: `SELECT COUNT(*) FROM message_catalogue GROUP BY language_code;`

#### Startup is slow (>500ms)

**Cause:** Large number of messages or slow database connection.

**Solution:**
1. Check message count: Is it >20K messages? Consider splitting by module.
2. Verify database connection: Is it local or remote? Remote adds network latency.
3. Add database indexes if needed (should be rare - startup is one-time SELECT).

#### Messages not updating after database changes

**Cause:** Cache not reloaded after database update.

**Solution:**
1. Call `reload()` method after updating database (see `message-catalogue-reload.md`)
2. Verify reload endpoint is working: Check logs for "Reloading" messages
3. As fallback, restart application to reload all messages

### JSON Implementation Issues

#### "Resource not found" error at startup

**Cause:** JSON file missing from classpath.

**Solution:**
1. Verify JSON file is in `resources` directory
2. Check file path starts with `/`: `/messages_en.json`
3. Verify file is included in JAR: `jar tf myapp.jar | grep messages`

#### Messages contain literal "%s" instead of arguments

**Cause:** Arguments not passed when retrieving message.

**Solution:**
```scala
// Wrong
catalogue(MessageId("welcome"))

// Correct
catalogue(UserMessage("welcome", "John"))
```

## FAQ

### Can I use both JSON and SQL implementations in the same application?

Yes, but it's not recommended. The `MessageCatalogueService` trait allows only one implementation per application. If you need both, create separate service instances with different names.

### How do I add a new language?

**JSON:** Add new JSON file (e.g., `/messages_de.json`) and update layer configuration.

**SQL:**
1. Run migration CLI for new language: `migration-cli --language=de --resource=/messages_de.json`
2. Update layer configuration to include new language: `SqlMessageCatalogueService.layer(Seq(EN, CS, DE), EN)`
3. Restart application

### What happens if a message key is missing?

Both implementations return the key as a fallback:

```scala
catalogue(MessageId("nonexistent.key"))
// Returns: "nonexistent.key"
```

This prevents blank messages in the UI.

### Can I update messages without restarting the application?

- **JSON:** No, messages are bundled with application code.
- **SQL:** Yes, update database and call `reload()` method. See `message-catalogue-reload.md`.

### How do I test with different messages?

**JSON:**
```scala
val testMessages = js.Dictionary("test.message" -> "Test value")
val testCatalogue = JsonMessageCatalogue(Language.EN, testMessages)
```

**SQL:**
```scala
// Use test database with test data
val testLayer = TestDatabase.layer >>>
  PostgreSQLMessageCatalogueRepository.layer >>>
  SqlMessageCatalogueService.layer(Seq(Language.EN), Language.EN)
```

### Is the SQL implementation production-ready?

Yes. The SQL implementation:
- Pre-loads all messages at startup (fail-fast if database unavailable)
- Uses in-memory cache for O(1) lookups (no database queries during runtime)
- Supports hot reload via `reload()` method
- Handles concurrent access safely via ZIO Ref
- Has comprehensive unit and integration tests

### What's the performance impact of using SQL vs JSON?

Negligible. Both implementations:
- Use in-memory Map for O(1) lookups
- Have identical runtime performance
- Use the same memory per language

The only difference is startup time: SQL adds ~100-200ms for 10K messages.
