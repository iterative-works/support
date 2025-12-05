-- PURPOSE: Create permissions table for storing ReBAC relation tuples
-- PURPOSE: Includes indexes for efficient permission queries and reverse lookups

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    relation VARCHAR(100) NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    object_id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permission UNIQUE (user_id, relation, namespace, object_id)
);

-- Index for getUserRelations queries (fetch all relations for user in namespace)
CREATE INDEX idx_permissions_user_namespace ON permissions(user_id, namespace);

-- Index for reverse lookups (find all users with permission to target)
CREATE INDEX idx_permissions_target ON permissions(namespace, object_id);

-- Index for hasRelation queries (check specific relation)
CREATE INDEX idx_permissions_user_relation_target ON permissions(user_id, relation, namespace, object_id);
