# Message Catalogue Deployment Runbook

## Overview

This runbook provides step-by-step procedures for deploying the SQL-backed Message Catalogue system to production, including validation, rollback, and troubleshooting.

## Pre-Deployment Checklist

### 1. Database Preparation

- [ ] **Database backup completed**
  ```bash
  # PostgreSQL
  pg_dump -h localhost -U postgres -d myapp > backup_$(date +%Y%m%d_%H%M%S).sql

  # MySQL/MariaDB
  mysqldump -h localhost -u root -p myapp > backup_$(date +%Y%m%d_%H%M%S).sql
  ```

- [ ] **Verify database connectivity**
  ```bash
  # PostgreSQL
  psql -h production-db.example.com -U appuser -d myapp -c "SELECT 1;"

  # MySQL/MariaDB
  mysql -h production-db.example.com -u appuser -p myapp -e "SELECT 1;"
  ```

- [ ] **Check database version compatibility**
  - PostgreSQL: 12.0 or higher
  - MySQL: 8.0 or higher
  - MariaDB: 10.5 or higher

### 2. Application Preparation

- [ ] **Test migration on staging environment**
  - Run Flyway migration
  - Run data migration
  - Start application
  - Verify message retrieval
  - Test reload functionality

- [ ] **Verify version number**
  ```bash
  git describe --tags
  # Should match the version being deployed
  ```

- [ ] **Code review approved**
  - All Phase 6 [reviewed] checkboxes marked
  - Operations team reviewed runbook
  - Technical lead approved deployment

- [ ] **Build application**
  ```bash
  mill core.jvm.assembly
  # Verify JAR created successfully
  ```

### 3. Message File Preparation

- [ ] **JSON message files available**
  ```bash
  ls -lh core/jvm/src/main/resources/messages_*.json
  # Verify files exist for all languages
  ```

- [ ] **Validate JSON syntax**
  ```bash
  for file in core/jvm/src/main/resources/messages_*.json; do
    echo "Validating $file"
    jq empty "$file" || echo "ERROR: Invalid JSON in $file"
  done
  ```

### 4. Rollback Plan Prepared

- [ ] Previous application version ready for quick rollback
- [ ] Database backup tested and verified
- [ ] Rollback procedures reviewed by team

## Deployment Steps

### Phase 1: Database Schema Migration

**Estimated Time:** 2-5 minutes

#### 1.1 Connect to Database Server

```bash
ssh deploy@production-db.example.com
```

#### 1.2 Run Flyway Migration

**PostgreSQL:**
```bash
mill core.jvm.runMain org.flywaydb.core.Flyway migrate \
  -url=jdbc:postgresql://localhost:5432/myapp \
  -user=appuser \
  -password=$DB_PASSWORD \
  -locations=filesystem:./migrations/postgresql
```

**MySQL/MariaDB:**
```bash
mill core.jvm.runMain org.flywaydb.core.Flyway migrate \
  -url=jdbc:mysql://localhost:3306/myapp \
  -user=appuser \
  -password=$DB_PASSWORD \
  -locations=filesystem:./migrations/mysql
```

#### 1.3 Verify Schema Migration

```sql
-- PostgreSQL
\dt message_catalogue

-- MySQL/MariaDB
SHOW TABLES LIKE 'message_catalogue';

-- Verify table structure
SELECT * FROM message_catalogue LIMIT 1;
```

**Expected Result:**
- Table `message_catalogue` exists
- Columns: `message_key`, `language_code`, `message_text`, `description`, `last_modified`
- Primary key on `(message_key, language_code)`

#### 1.4 Phase 1 Success Criteria

- [ ] Flyway migration completed without errors
- [ ] Table `message_catalogue` exists with correct schema
- [ ] Database logs show no errors
- [ ] `flyway_schema_history` table updated with new migration

### Phase 2: Data Migration

**Estimated Time:** 5-15 minutes (depending on message count)

#### 2.1 Identify Languages to Migrate

```bash
# List available JSON files
ls core/jvm/src/main/resources/messages_*.json
# Example output:
# messages_en.json
# messages_cs.json
# messages_de.json
```

#### 2.2 Run Migration CLI for Each Language

**PostgreSQL:**

```bash
# English
mill core.jvm.runMain works.iterative.sqldb.postgresql.migration.MessageCatalogueMigrationCLI \
  --language=en \
  --resource=/messages_en.json

# Czech
mill core.jvm.runMain works.iterative.sqldb.postgresql.migration.MessageCatalogueMigrationCLI \
  --language=cs \
  --resource=/messages_cs.json

# German
mill core.jvm.runMain works.iterative.sqldb.postgresql.migration.MessageCatalogueMigrationCLI \
  --language=de \
  --resource=/messages_de.json
```

**MySQL/MariaDB:**

```bash
# English
mill sqldb-mysql.runMain works.iterative.sqldb.mysql.migration.MySQLMessageCatalogueMigrationCLI \
  --language=en \
  --resource=/messages_en.json

# Czech
mill sqldb-mysql.runMain works.iterative.sqldb.mysql.migration.MySQLMessageCatalogueMigrationCLI \
  --language=cs \
  --resource=/messages_cs.json

# German
mill sqldb-mysql.runMain works.iterative.sqldb.mysql.migration.MySQLMessageCatalogueMigrationCLI \
  --language=de \
  --resource=/messages_de.json
```

#### 2.3 Verify Data Migration

```sql
-- Check message counts per language
SELECT language_code, COUNT(*) as message_count
FROM message_catalogue
GROUP BY language_code
ORDER BY language_code;

-- Expected output example:
-- language_code | message_count
-- cs            | 1198
-- de            | 1156
-- en            | 1247

-- Verify sample messages
SELECT message_key, language_code, message_text
FROM message_catalogue
WHERE message_key = 'welcome'
ORDER BY language_code;
```

#### 2.4 Phase 2 Success Criteria

- [ ] Migration CLI completed for all languages without errors
- [ ] Message counts match JSON file counts (within 1-2% for formatting differences)
- [ ] Sample messages retrieved correctly from database
- [ ] No duplicate keys per language (enforced by primary key constraint)

### Phase 3: Application Configuration Update

**Estimated Time:** 5-10 minutes

#### 3.1 Update Application Configuration

Edit your application's layer composition to use SQL implementation:

**Before (JSON):**
```scala
val appLayer =
  JsonMessageCatalogueService.layer(Language.EN)
```

**After (SQL):**
```scala
import works.iterative.core.Language
import works.iterative.sqldb.postgresql.{PostgreSQLDatabaseSupport, PostgreSQLMessageCatalogueRepository}
import works.iterative.core.service.impl.SqlMessageCatalogueService

val languages = Seq(Language.EN, Language.CS, Language.DE)
val defaultLanguage = Language.EN

val appLayer =
  PostgreSQLDatabaseSupport.layer >>>
  PostgreSQLMessageCatalogueRepository.layer >>>
  SqlMessageCatalogueService.layer(languages, defaultLanguage)
```

#### 3.2 Update Environment Variables

Set database connection parameters:

```bash
export DB_HOST=production-db.example.com
export DB_PORT=5432
export DB_NAME=myapp
export DB_USER=appuser
export DB_PASSWORD=$SECURE_PASSWORD
```

Or use configuration file:

```hocon
# application.conf
database {
  host = "production-db.example.com"
  port = 5432
  database = "myapp"
  user = "appuser"
  password = ${DB_PASSWORD}
}

messageCatalogue {
  languages = ["en", "cs", "de"]
  defaultLanguage = "en"
}
```

#### 3.3 Build and Package Application

```bash
# Build application with new configuration
mill core.jvm.assembly

# Verify JAR contains SQL classes
jar tf out/core/jvm/assembly.dest/out.jar | grep SqlMessageCatalogue
```

#### 3.4 Phase 3 Success Criteria

- [ ] Configuration updated to use `SqlMessageCatalogueService.layer`
- [ ] Database connection parameters configured
- [ ] Application builds successfully
- [ ] JAR contains SQL implementation classes

### Phase 4: Application Deployment

**Estimated Time:** 5-10 minutes

#### 4.1 Stop Current Application

```bash
# Using systemd
sudo systemctl stop myapp

# Or using process manager
pm2 stop myapp

# Verify application stopped
curl -f http://localhost:8080/health || echo "Application stopped"
```

#### 4.2 Deploy New Application Version

```bash
# Copy new JAR
scp out/core/jvm/assembly.dest/out.jar deploy@production-app:/opt/myapp/myapp.jar

# Or using deployment tool
ansible-playbook deploy-myapp.yml --extra-vars "version=v2.0.0"
```

#### 4.3 Start Application

```bash
# Using systemd
sudo systemctl start myapp

# Or using process manager
pm2 start myapp

# Verify application started
sleep 5
curl -f http://localhost:8080/health && echo "Application started"
```

#### 4.4 Monitor Startup Logs

```bash
# Watch application logs for message pre-load
tail -f /var/log/myapp/application.log

# Expected log output:
# [info] Pre-loading message catalogues for 3 languages
# [info] Loaded en: 1247 messages
# [info] Loaded cs: 1198 messages
# [info] Loaded de: 1156 messages
# [info] Message catalogue service initialized: 3601 total messages
# [info] Application started successfully
```

**Warning Signs:**
- Startup takes longer than 5 seconds
- Error messages about database connection
- Application crashes or restarts repeatedly

If you see warnings, proceed to Troubleshooting section.

#### 4.5 Phase 4 Success Criteria

- [ ] Application started without errors
- [ ] Logs show successful message pre-load for all languages
- [ ] Health check endpoint returns 200 OK
- [ ] Application responds to requests within 2 seconds

### Phase 5: Post-Deployment Validation

**Estimated Time:** 10-15 minutes

#### 5.1 Verify Message Retrieval

**Test via HTTP endpoint:**
```bash
# Test English messages
curl http://localhost:8080/api/message?key=welcome
# Expected: "Welcome!"

# Test Czech messages
curl http://localhost:8080/api/message?key=welcome&lang=cs
# Expected: "Vítejte!"

# Test German messages
curl http://localhost:8080/api/message?key=welcome&lang=de
# Expected: "Willkommen!"
```

**Test via application UI:**
1. Open application in browser
2. Verify welcome message displays correctly
3. Switch language to Czech, verify messages change
4. Switch language to German, verify messages change

#### 5.2 Test Reload Functionality

**Update a message in database:**
```sql
UPDATE message_catalogue
SET message_text = 'Welcome to our application!'
WHERE message_key = 'welcome' AND language_code = 'en';
```

**Call reload endpoint:**
```bash
curl -X POST http://localhost:8080/admin/messages/reload \
  -H "Content-Type: application/json" \
  -d '{"language": "en"}'
```

**Verify updated message:**
```bash
curl http://localhost:8080/api/message?key=welcome
# Expected: "Welcome to our application!"
```

#### 5.3 Check Application Logs

```bash
tail -100 /var/log/myapp/application.log

# Look for:
# - Message load counts match expected values
# - No error messages
# - Reload operations complete successfully
# - Response times < 200ms
```

#### 5.4 Monitor Application Performance

```bash
# Check memory usage
ps aux | grep java | grep myapp

# Check response times
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/api/message?key=welcome

# Monitor error rates
tail -f /var/log/myapp/application.log | grep ERROR
```

#### 5.5 Phase 5 Success Criteria

- [ ] Messages retrieved correctly in all languages
- [ ] Reload functionality works as expected
- [ ] Application logs show no errors
- [ ] Response times within acceptable range (<200ms for message retrieval)
- [ ] Memory usage stable and within limits
- [ ] No increase in error rates

## Rollback Procedures

### Scenario 1: Application-Level Rollback (Recommended)

**Use when:** Application fails to start or messages not loading correctly

**Estimated Time:** 5-10 minutes

**Steps:**

1. Stop current application:
   ```bash
   sudo systemctl stop myapp
   ```

2. Revert configuration to use JSON implementation:
   ```scala
   // Restore this in code or config
   val appLayer = JsonMessageCatalogueService.layer(Language.EN)
   ```

3. Deploy previous application version:
   ```bash
   scp backup/myapp-v1.9.0.jar deploy@production-app:/opt/myapp/myapp.jar
   ```

4. Start application:
   ```bash
   sudo systemctl start myapp
   ```

5. Verify application working:
   ```bash
   curl http://localhost:8080/health
   ```

**Result:** Application runs with JSON messages (previous behavior), database unchanged for later retry.

### Scenario 2: Database Rollback

**Use when:** Data corruption or incorrect data migration

**Estimated Time:** 15-30 minutes

**Steps:**

1. Stop application:
   ```bash
   sudo systemctl stop myapp
   ```

2. Restore database from backup:
   ```bash
   # PostgreSQL
   psql -h localhost -U postgres -d myapp < backup_20240315_140000.sql

   # MySQL/MariaDB
   mysql -h localhost -u root -p myapp < backup_20240315_140000.sql
   ```

3. Verify database restored:
   ```sql
   SELECT COUNT(*) FROM message_catalogue;
   -- Should match counts before migration attempt
   ```

4. Choose next step:
   - Option A: Retry migration with fixes
   - Option B: Rollback to JSON (see Scenario 1)

### Scenario 3: Partial Rollback (Keep Schema, Revert to JSON)

**Use when:** Schema is fine but want to revert application to JSON temporarily

**Estimated Time:** 5-10 minutes

**Steps:**

1. Stop application:
   ```bash
   sudo systemctl stop myapp
   ```

2. Revert application configuration (see Scenario 1 step 2)

3. Deploy application with JSON configuration

4. Start application:
   ```bash
   sudo systemctl start myapp
   ```

5. Verify application working with JSON messages

**Result:** Application uses JSON messages, database table remains (can retry SQL migration later without schema migration).

### Scenario 4: Complete Rollback (Schema and Application)

**Use when:** Major issues require complete removal of SQL implementation

**Estimated Time:** 20-40 minutes

**Steps:**

1. Stop application (see Scenario 1 step 1)

2. Remove message catalogue table:
   ```sql
   DROP TABLE message_catalogue;
   ```

3. Revert Flyway migration:
   ```sql
   DELETE FROM flyway_schema_history
   WHERE version = '2.0.0';  -- Adjust version number
   ```

4. Restore application to JSON (see Scenario 1 steps 2-5)

5. Verify complete rollback:
   - Application uses JSON messages
   - Database has no message_catalogue table
   - Flyway history shows pre-migration state

## Troubleshooting

### Issue: Application Fails to Start

**Symptoms:**
- Application crashes during startup
- Logs show "Failed to load messages"
- Health check returns 503

**Diagnosis:**

1. Check logs for specific error:
   ```bash
   tail -100 /var/log/myapp/application.log | grep ERROR
   ```

2. Common error messages:
   - "Connection refused": Database not accessible
   - "Table 'message_catalogue' doesn't exist": Schema migration not run
   - "Failed to initialize": Configuration error

**Solutions:**

**Database not accessible:**
```bash
# Verify database is running
sudo systemctl status postgresql

# Test connection manually
psql -h localhost -U appuser -d myapp

# Check firewall rules
sudo iptables -L | grep 5432

# Verify credentials
cat /opt/myapp/application.conf | grep database
```

**Schema migration not run:**
```bash
# Check Flyway history
psql -h localhost -U appuser -d myapp -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Re-run migration if needed
mill core.jvm.runMain org.flywaydb.core.Flyway migrate -url=jdbc:postgresql://localhost:5432/myapp
```

**Configuration error:**
```bash
# Verify configuration file syntax
cat /opt/myapp/application.conf

# Check environment variables
env | grep DB_

# Verify layer composition
grep -A 10 "SqlMessageCatalogueService.layer" /path/to/config.scala
```

### Issue: Slow Startup (>5 seconds)

**Symptoms:**
- Application takes longer than expected to start
- Logs show long pause between "Pre-loading" and "Loaded" messages

**Diagnosis:**

```bash
# Check message counts
psql -h localhost -U appuser -d myapp -c \
  "SELECT language_code, COUNT(*) FROM message_catalogue GROUP BY language_code;"

# Measure database query time
psql -h localhost -U appuser -d myapp -c \
  "EXPLAIN ANALYZE SELECT * FROM message_catalogue WHERE language_code = 'en';"
```

**Solutions:**

**Large message count (>20K per language):**
- Consider splitting messages by module or feature
- Optimize database query (add index if needed)
- Increase JVM heap size if memory is constrained

**Slow database connection:**
- Check network latency to database server
- Consider local database replica for read queries
- Increase database connection pool size

**Query performance:**
```sql
-- Add index if needed (should rarely be necessary)
CREATE INDEX idx_message_catalogue_language ON message_catalogue(language_code);

-- Verify index usage
EXPLAIN ANALYZE SELECT * FROM message_catalogue WHERE language_code = 'en';
```

### Issue: Messages Not Updating After Reload

**Symptoms:**
- Database updated but application shows old messages
- Reload endpoint returns 200 OK but messages unchanged

**Diagnosis:**

```bash
# Check reload logs
grep "Reloading" /var/log/myapp/application.log | tail -20

# Verify database has new values
psql -h localhost -U appuser -d myapp -c \
  "SELECT message_key, message_text FROM message_catalogue WHERE message_key = 'welcome';"

# Test reload endpoint
curl -v -X POST http://localhost:8080/admin/messages/reload \
  -H "Content-Type: application/json" \
  -d '{"language": "en"}'
```

**Solutions:**

**Reload not called:**
```bash
# Call reload after database update
curl -X POST http://localhost:8080/admin/messages/reload \
  -H "Content-Type: application/json" \
  -d '{"language": "en"}'
```

**Cache not updated:**
```bash
# Check for reload errors in logs
grep -A 5 "reload" /var/log/myapp/application.log | grep ERROR

# As fallback, restart application
sudo systemctl restart myapp
```

**Wrong language specified:**
```bash
# Reload all languages instead of specific language
curl -X POST http://localhost:8080/admin/messages/reload \
  -H "Content-Type: application/json" \
  -d '{"language": null}'
```

### Issue: High Memory Usage

**Symptoms:**
- Memory usage higher than expected
- OutOfMemoryError in logs
- Application performance degrades over time

**Diagnosis:**

```bash
# Check heap usage
jmap -heap $(pgrep -f myapp)

# Count messages
psql -h localhost -U appuser -d myapp -c \
  "SELECT COUNT(*), SUM(LENGTH(message_text)) FROM message_catalogue;"

# Monitor over time
while true; do
  ps aux | grep myapp | grep -v grep
  sleep 5
done
```

**Solutions:**

**Expected memory usage:**
- ~500KB per 5K messages per language
- With 3 languages × 5K messages = ~1.5MB for message cache
- If significantly higher, investigate other causes

**Too many messages:**
- Consider splitting message catalogue by module
- Lazy-load languages on demand instead of pre-loading all

**Memory leak:**
- Monitor for memory growth over time
- Check for duplicate cache instances
- Verify reload doesn't create memory leaks

### Issue: Reload Endpoint Returns 404

**Symptoms:**
- `curl http://localhost:8080/admin/messages/reload` returns 404
- Reload functionality not available

**Diagnosis:**

```bash
# Check if endpoint is registered
curl http://localhost:8080/admin/messages/ -v

# Check application routes
grep -r "reload" /path/to/application/routes.scala

# Verify SqlMessageCatalogueService is used
grep "SqlMessageCatalogueService" /var/log/myapp/application.log
```

**Solutions:**

**Endpoint not implemented:**
- Add reload endpoint to application routes (see `message-catalogue-reload.md`)
- Restart application after adding endpoint

**Wrong service implementation:**
- Verify application uses `SqlMessageCatalogueService`, not `JsonMessageCatalogueService`
- JSON implementation doesn't support reload (reload() method doesn't exist)

**Authorization required:**
- Check if endpoint requires authentication
- Add proper authorization headers to request

## Monitoring Recommendations

### Metrics to Track

1. **Startup Metrics:**
   - Time to pre-load messages (should be <200ms for 10K messages)
   - Message counts per language
   - Pre-load success/failure rate

2. **Runtime Metrics:**
   - Message retrieval latency (should be <1ms - pure map lookup)
   - Cache hit rate (should be 100% - all messages pre-loaded)
   - Reload duration (should be <200ms)
   - Reload frequency (track how often reload is called)

3. **Error Metrics:**
   - Database connection failures
   - Reload failures
   - Message key not found (indicates missing translations)

### Logging Configuration

```scala
// Log all message pre-load operations
logger.info(s"Pre-loading message catalogues for ${languages.size} languages")
logger.info(s"Loaded $language: ${messages.size} messages")

// Log reload operations
logger.info(s"Reloading $language: ${messages.size} messages")

// Log errors
logger.error(s"Failed to load messages: ${error.getMessage}", error)
```

### Alerting Rules

**Critical Alerts:**
- Application fails to start (message pre-load failure)
- Reload fails 3+ times in 1 hour
- Database connection unavailable

**Warning Alerts:**
- Startup time >500ms (investigate database performance)
- Reload duration >500ms (investigate message count or database)
- Message key not found >100 times/hour (missing translations)

### Dashboard Metrics

Create dashboard with:
- Message count per language (gauge)
- Pre-load duration (histogram)
- Reload duration (histogram)
- Reload frequency (counter)
- Error rate (counter)

## Success Criteria

### Phase-by-Phase Success

- [x] **Phase 1:** Schema migration completed, table exists with correct structure
- [x] **Phase 2:** Data migration completed, message counts match expected values
- [x] **Phase 3:** Application configuration updated, builds successfully
- [x] **Phase 4:** Application started, logs show successful message pre-load
- [x] **Phase 5:** Messages retrieved correctly, reload functionality works

### Overall Deployment Success

- [ ] Application running in production with SQL message catalogue
- [ ] All messages retrievable in all languages
- [ ] Reload functionality tested and working
- [ ] No errors in application logs
- [ ] Performance metrics within acceptable range
- [ ] Rollback plan tested on staging
- [ ] Team trained on reload and monitoring procedures

## Post-Deployment Tasks

### Immediate (Within 24 Hours)

- [ ] Monitor application logs for any errors or warnings
- [ ] Verify message retrieval performance metrics
- [ ] Test reload functionality in production
- [ ] Confirm monitoring alerts are working

### Short-Term (Within 1 Week)

- [ ] Review deployment process and update runbook with lessons learned
- [ ] Train support team on troubleshooting procedures
- [ ] Set up automated monitoring and alerting
- [ ] Document any production-specific configuration

### Long-Term (Within 1 Month)

- [ ] Review message update workflow with content team
- [ ] Optimize reload frequency based on actual usage patterns
- [ ] Consider implementing admin UI for message management
- [ ] Plan migration of additional message catalogues if applicable

## Escalation Contacts

### Primary Contacts

**Technical Lead:**
- Name: [Your Tech Lead]
- Email: tech.lead@example.com
- Phone: +1-xxx-xxx-xxxx
- Timezone: UTC+1

**Database Administrator:**
- Name: [Your DBA]
- Email: dba@example.com
- Phone: +1-xxx-xxx-xxxx
- Timezone: UTC+1

**Operations Manager:**
- Name: [Your Ops Manager]
- Email: ops.manager@example.com
- Phone: +1-xxx-xxx-xxxx
- Timezone: UTC+1

### Escalation Procedure

1. **Level 1:** Try troubleshooting steps in this runbook (10-15 minutes)
2. **Level 2:** Contact Technical Lead via Slack/email (response time: <30 minutes)
3. **Level 3:** Call Technical Lead if no response (response time: <10 minutes)
4. **Level 4:** Execute rollback procedure (Scenario 1) to restore service
5. **Level 5:** Escalate to Operations Manager for incident coordination

## Appendix

### Useful Commands Reference

```bash
# Check application status
sudo systemctl status myapp

# View recent logs
tail -100 /var/log/myapp/application.log

# View real-time logs
tail -f /var/log/myapp/application.log

# Check database connection
psql -h localhost -U appuser -d myapp -c "SELECT 1;"

# Count messages per language
psql -h localhost -U appuser -d myapp -c \
  "SELECT language_code, COUNT(*) FROM message_catalogue GROUP BY language_code;"

# Test reload endpoint
curl -X POST http://localhost:8080/admin/messages/reload \
  -H "Content-Type: application/json" \
  -d '{"language": null}'

# Check memory usage
ps aux | grep myapp | grep -v grep | awk '{print $6}'

# Monitor error rate
watch 'tail -100 /var/log/myapp/application.log | grep -c ERROR'
```

### Configuration Templates

**Database Configuration (application.conf):**
```hocon
database {
  host = ${DB_HOST}
  port = ${DB_PORT}
  database = ${DB_NAME}
  user = ${DB_USER}
  password = ${DB_PASSWORD}

  connectionPool {
    minimumIdle = 2
    maximumPoolSize = 10
    connectionTimeout = 30000
  }
}

messageCatalogue {
  languages = ["en", "cs", "de"]
  defaultLanguage = "en"
}
```

**Systemd Service File (myapp.service):**
```ini
[Unit]
Description=My Application
After=network.target postgresql.service

[Service]
Type=simple
User=myapp
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -jar /opt/myapp/myapp.jar
Restart=on-failure
RestartSec=10

Environment="DB_HOST=localhost"
Environment="DB_PORT=5432"
Environment="DB_NAME=myapp"
Environment="DB_USER=appuser"
EnvironmentFile=/opt/myapp/database.env

[Install]
WantedBy=multi-user.target
```

### Related Documentation

- [Message Catalogue Implementation Guide](./message-catalogue-implementation-guide.md) - Choose JSON vs SQL
- [Message Catalogue Reload Mechanism](./message-catalogue-reload.md) - Hot reload documentation
- [Flyway Migration Documentation](https://flywaydb.org/documentation/) - Database migration tool
- [ZIO Documentation](https://zio.dev/) - Effect system documentation
