# Code Review - Phase 1: Database Foundation (Iteration 2)

**Iteration:** 2/3
**Timestamp:** 2025-11-13
**Phase:** Phase 1: Database Foundation
**Issue:** IWSD-76

## Files Reviewed

- build.mill
- core/jvm/src/main/resources/db/migration/V1__create_message_catalogue.sql
- sqldb/src/main/scala/works/iterative/sqldb/MessageCatalogueEntity.scala
- sqldb/src/test/scala/works/iterative/sqldb/MessageCatalogueAuditTriggerSpec.scala
- sqldb/src/test/scala/works/iterative/sqldb/MessageCatalogueEntitySpec.scala
- sqldb/src/test/scala/works/iterative/sqldb/MessageCatalogueMigrationSpec.scala

## Review Summary

- **Critical issues**: 2
- **Warnings**: 4
- **Suggestions**: 3

## Critical Issues (Must Fix)

### 1. Audit Trigger Should Use NEW.updated_at for Consistency

**Problem:** Audit trigger uses `CURRENT_TIMESTAMP` for `changed_at` but should use `NEW.updated_at` to ensure timestamps match exactly.

**Fix:** Change line 61 in migration from:
```sql
CURRENT_TIMESTAMP,
```
to:
```sql
NEW.updated_at,
```

### 2. Missing Test for updated_at Trigger

**Problem:** The auto-update trigger for `updated_at` added in Iteration 2 has no tests.

**Fix:** Add test verifying updated_at changes on UPDATE operations.

## Warnings (Should Fix)

1. Inconsistent use of `.transact` vs `.connect` in tests
2. Magic numbers in test data ('test.key1', 'test.key2')
3. Missing index on message_catalogue_history(message_key, language)
4. No test for FK constraint ON DELETE RESTRICT

## Positive Findings

- All Iteration 1 critical issues fixed ✓
- Comprehensive audit trigger tests
- Proper test isolation with clean/migrate
- Good entity design with Magnum annotations
- All 13 tests passing

## Next Steps

1. Fix audit trigger to use NEW.updated_at
2. Add test for updated_at trigger
3. Consider adding FK constraint test
