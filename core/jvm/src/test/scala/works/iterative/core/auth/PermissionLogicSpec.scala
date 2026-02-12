// PURPOSE: Test specification for PermissionLogic pure domain functions
// PURPOSE: Validates FCIS functional core permission checking without ZIO effects

package works.iterative.core.auth

import zio.test.*


object PermissionLogicSpec extends ZIOSpecDefault:

  def spec = suite("PermissionLogicSpec")(
    test("isAllowed with direct permission returns true") {
      val userId = UserId.unsafe("user123")
      val target = PermissionTarget.unsafe("document", "doc456")
      val action = PermissionOp.unsafe("edit")

      val tuples = Set(
        RelationTuple(userId, "edit", target)
      )

      val config = PermissionConfig(namespaces = Map.empty)

      val result = PermissionLogic.isAllowed(userId, action, target, tuples, config)

      assertTrue(result)
    },

    test("isAllowed with inherited permission returns true") {
      val userId = UserId.unsafe("user123")
      val target = PermissionTarget.unsafe("document", "doc456")
      val viewAction = PermissionOp.unsafe("view")

      // User has "owner" relation, which implies "view" via config
      val tuples = Set(
        RelationTuple(userId, "owner", target)
      )

      val config = PermissionConfig(
        namespaces = Map(
          "document" -> NamespaceConfig(
            implications = Map(
              "owner" -> Set("edit", "view", "delete")
            )
          )
        )
      )

      val result = PermissionLogic.isAllowed(userId, viewAction, target, tuples, config)

      assertTrue(result)
    },

    test("isAllowed with denied permission returns false") {
      val userId = UserId.unsafe("user123")
      val target = PermissionTarget.unsafe("document", "doc456")
      val deleteAction = PermissionOp.unsafe("delete")

      // User has "viewer" relation, which does NOT imply "delete"
      val tuples = Set(
        RelationTuple(userId, "viewer", target)
      )

      val config = PermissionConfig(
        namespaces = Map(
          "document" -> NamespaceConfig(
            implications = Map(
              "viewer" -> Set("view")
            )
          )
        )
      )

      val result = PermissionLogic.isAllowed(userId, deleteAction, target, tuples, config)

      assertTrue(!result)
    },

    test("isAllowed returns false when no matching tuples") {
      val userId = UserId.unsafe("user123")
      val target = PermissionTarget.unsafe("document", "doc456")
      val action = PermissionOp.unsafe("edit")

      val tuples = Set.empty[RelationTuple]
      val config = PermissionConfig(namespaces = Map.empty)

      val result = PermissionLogic.isAllowed(userId, action, target, tuples, config)

      assertTrue(!result)
    },

    test("listAllowed returns resources user can access") {
      val userId = UserId.unsafe("user123")
      val action = PermissionOp.unsafe("view")

      val doc1 = PermissionTarget.unsafe("document", "doc1")
      val doc2 = PermissionTarget.unsafe("document", "doc2")
      val doc3 = PermissionTarget.unsafe("document", "doc3")

      val tuples = Set(
        RelationTuple(userId, "owner", doc1),    // owner implies view
        RelationTuple(userId, "viewer", doc2),   // viewer implies view
        // doc3 has no relation for this user
        RelationTuple(UserId.unsafe("other"), "viewer", doc3)
      )

      val config = PermissionConfig(
        namespaces = Map(
          "document" -> NamespaceConfig(
            implications = Map(
              "owner" -> Set("edit", "view", "delete"),
              "viewer" -> Set("view")
            )
          )
        )
      )

      val result = PermissionLogic.listAllowed(userId, action, "document", tuples, config)

      assertTrue(
        result.contains("doc1"),
        result.contains("doc2"),
        !result.contains("doc3"),
        result.size == 2
      )
    },

    test("listAllowed returns empty set when user has no access") {
      val userId = UserId.unsafe("user123")
      val action = PermissionOp.unsafe("view")

      val tuples = Set(
        RelationTuple(UserId.unsafe("other"), "viewer", PermissionTarget.unsafe("document", "doc1"))
      )

      val config = PermissionConfig(namespaces = Map.empty)

      val result = PermissionLogic.listAllowed(userId, action, "document", tuples, config)

      assertTrue(result.isEmpty)
    },

    test("listAllowed filters by namespace") {
      val userId = UserId.unsafe("user123")
      val action = PermissionOp.unsafe("view")

      val doc1 = PermissionTarget.unsafe("document", "doc1")
      val folder1 = PermissionTarget.unsafe("folder", "folder1")

      val tuples = Set(
        RelationTuple(userId, "viewer", doc1),
        RelationTuple(userId, "viewer", folder1)
      )

      val config = PermissionConfig(
        namespaces = Map(
          "document" -> NamespaceConfig(
            implications = Map("viewer" -> Set("view"))
          ),
          "folder" -> NamespaceConfig(
            implications = Map("viewer" -> Set("view"))
          )
        )
      )

      val result = PermissionLogic.listAllowed(userId, action, "document", tuples, config)

      assertTrue(
        result.contains("doc1"),
        !result.contains("folder1"),
        result.size == 1
      )
    }
  )
end PermissionLogicSpec
