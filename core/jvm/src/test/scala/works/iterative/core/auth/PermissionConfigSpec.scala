// PURPOSE: Test specification for PermissionConfig
// PURPOSE: Validates permission inheritance rules and computed relations

package works.iterative.core.auth

import zio.test.*


object PermissionConfigSpec extends ZIOSpecDefault:

  def spec = suite("PermissionConfigSpec")(
    test("defines namespace with owner→editor→viewer hierarchy") {
      val namespaceConfig = NamespaceConfig(
        implications = Map(
          "owner" -> Set("edit", "view", "delete"),
          "editor" -> Set("edit", "view"),
          "viewer" -> Set("view")
        )
      )

      val config = PermissionConfig(
        namespaces = Map("document" -> namespaceConfig)
      )

      assertTrue(
        config.namespaces.contains("document"),
        config.namespaces("document").implications("owner").contains("edit"),
        config.namespaces("document").implications("owner").contains("view"),
        config.namespaces("document").implications("owner").contains("delete")
      )
    },

    test("computedRelations returns implied relations for owner") {
      val namespaceConfig = NamespaceConfig(
        implications = Map(
          "owner" -> Set("edit", "view", "delete"),
          "editor" -> Set("edit", "view"),
          "viewer" -> Set("view")
        )
      )

      val config = PermissionConfig(
        namespaces = Map("document" -> namespaceConfig)
      )

      val computed = config.computedRelations("document", "owner")

      assertTrue(
        computed.contains("owner"),  // Direct relation
        computed.contains("edit"),   // Implied
        computed.contains("view"),   // Implied
        computed.contains("delete")  // Implied
      )
    },

    test("computedRelations returns only direct permission for namespace without implications") {
      val namespaceConfig = NamespaceConfig(
        implications = Map.empty
      )

      val config = PermissionConfig(
        namespaces = Map("simple" -> namespaceConfig)
      )

      val computed = config.computedRelations("simple", "custom")

      assertTrue(
        computed == Set("custom"),  // Only the direct relation
        computed.size == 1
      )
    },

    test("computedRelations limits depth to prevent DoS") {
      // Create a very deep hierarchy
      val namespaceConfig = NamespaceConfig(
        implications = Map(
          "level0" -> Set("level1"),
          "level1" -> Set("level2"),
          "level2" -> Set("level3"),
          "level3" -> Set("level4"),
          "level4" -> Set("level5"),
          "level5" -> Set("level6"),
          "level6" -> Set("level7"),
          "level7" -> Set("level8"),
          "level8" -> Set("level9"),
          "level9" -> Set("level10"),
          "level10" -> Set("level11"),  // This should be cutoff
          "level11" -> Set("level12")
        )
      )

      val config = PermissionConfig(
        namespaces = Map("deep" -> namespaceConfig)
      )

      val computed = config.computedRelations("deep", "level0")

      // Should include level0 through level10 (11 levels = depth 10)
      // but not level11 or beyond due to maxInheritanceDepth = 10
      assertTrue(
        computed.contains("level0"),
        computed.contains("level10"),
        computed.size <= 11  // At most 11 relations (level0 through level10)
      )
    },

    test("computedRelations handles transitive implications") {
      val namespaceConfig = NamespaceConfig(
        implications = Map(
          "owner" -> Set("editor"),
          "editor" -> Set("viewer"),
          "viewer" -> Set("read")
        )
      )

      val config = PermissionConfig(
        namespaces = Map("document" -> namespaceConfig)
      )

      val computed = config.computedRelations("document", "owner")

      assertTrue(
        computed.contains("owner"),
        computed.contains("editor"),   // Direct implication
        computed.contains("viewer"),   // Transitive via editor
        computed.contains("read")      // Transitive via viewer
      )
    },

    test("computedRelations returns only direct relation for unknown namespace") {
      val config = PermissionConfig(namespaces = Map.empty)

      val computed = config.computedRelations("unknown", "someRelation")

      assertTrue(
        computed == Set("someRelation")
      )
    }
  )
end PermissionConfigSpec
