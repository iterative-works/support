// PURPOSE: Test suite for PermissionTarget validation logic
// PURPOSE: Validates all validation rules for namespace:id format and edge cases

package works.iterative.core.auth

import zio.test.*
import zio.test.Assertion.*

object PermissionTargetSpec extends ZIOSpecDefault:

  def spec = suite("PermissionTargetSpec")(
    test("apply with valid namespace:id succeeds") {
      val result = PermissionTarget("document:123")
      assertTrue(result.isSuccess)
    },

    test("apply with empty namespace fails") {
      val result = PermissionTarget(":123")
      assertTrue(result.isFailure)
    },

    test("apply with empty id fails") {
      val result = PermissionTarget("document:")
      assertTrue(result.isFailure)
    },

    test("apply with whitespace-only namespace fails") {
      val result = PermissionTarget("  :123")
      assertTrue(result.isFailure)
    },

    test("apply with whitespace-only id fails") {
      val result = PermissionTarget("document:  ")
      assertTrue(result.isFailure)
    },

    test("apply with valid relation format succeeds") {
      val result = PermissionTarget("document:123#parent")
      assertTrue(result.isSuccess)
    },

    test("apply extracts namespace correctly") {
      val result = PermissionTarget("document:123")
      result match {
        case zio.prelude.Validation.Success(_, target) =>
          assertTrue(target.namespace == "document")
        case _ =>
          assertTrue(false)
      }
    },

    test("apply extracts id correctly") {
      val result = PermissionTarget("document:123")
      result match {
        case zio.prelude.Validation.Success(_, target) =>
          assertTrue(target.value == "123")
        case _ =>
          assertTrue(false)
      }
    },

    test("apply extracts relation correctly when present") {
      val result = PermissionTarget("document:123#parent")
      result match {
        case zio.prelude.Validation.Success(_, target) =>
          assertTrue(target.rel.contains("parent"))
        case _ =>
          assertTrue(false)
      }
    },

    test("apply sets relation to None when not present") {
      val result = PermissionTarget("document:123")
      result match {
        case zio.prelude.Validation.Success(_, target) =>
          assertTrue(target.rel.isEmpty)
        case _ =>
          assertTrue(false)
      }
    },

    test("apply without colon separator fails") {
      val result = PermissionTarget("document123")
      assertTrue(result.isFailure)
    },

    test("apply(namespace, id) with valid inputs succeeds") {
      val result = PermissionTarget("document", "123")
      assertTrue(result.isSuccess)
    },

    test("apply(namespace, id) with empty namespace fails") {
      val result = PermissionTarget("", "123")
      assertTrue(result.isFailure)
    },

    test("apply(namespace, id) with empty id fails") {
      val result = PermissionTarget("document", "")
      assertTrue(result.isFailure)
    },

    test("apply(namespace, id, rel) with valid relation succeeds") {
      val result = PermissionTarget("document", "123", Some("parent"))
      assertTrue(result.isSuccess)
    },

    test("apply(namespace, id, rel) with None relation succeeds") {
      val result = PermissionTarget("document", "123", None)
      assertTrue(result.isSuccess)
    },

    test("unsafe(target) with valid target succeeds") {
      val target = PermissionTarget.unsafe("document:123")
      assertTrue(target.namespace == "document" && target.value == "123")
    },

    test("unsafe(target) with missing colon throws") {
      val result = try {
        PermissionTarget.unsafe("document123")
        false
      } catch {
        case _: IllegalArgumentException => true
      }
      assertTrue(result)
    },

    test("unsafe(namespace, id) with valid inputs succeeds") {
      val target = PermissionTarget.unsafe("document", "123")
      assertTrue(target.namespace == "document" && target.value == "123")
    },

    test("unsafe(namespace, id) with empty namespace throws") {
      val result = try {
        PermissionTarget.unsafe("", "123")
        false
      } catch {
        case _: IllegalArgumentException => true
      }
      assertTrue(result)
    },

    test("unsafe(namespace, id) with empty id throws") {
      val result = try {
        PermissionTarget.unsafe("document", "")
        false
      } catch {
        case _: IllegalArgumentException => true
      }
      assertTrue(result)
    },

    test("unsafe(namespace, id) with namespace containing colon throws") {
      val result = try {
        PermissionTarget.unsafe("doc:ument", "123")
        false
      } catch {
        case _: IllegalArgumentException => true
      }
      assertTrue(result)
    },

    test("unsafe(namespace, id) with id containing colon succeeds") {
      val target = PermissionTarget.unsafe("document", "http://example.com")
      assertTrue(
        target.namespace == "document" &&
        target.value == "http://example.com"
      )
    },

    test("unsafe(namespace, id, rel) with valid relation succeeds") {
      val target = PermissionTarget.unsafe("document", "123", Some("parent"))
      assertTrue(
        target.namespace == "document" &&
        target.value == "123" &&
        target.rel.contains("parent")
      )
    },

    test("unsafe(namespace, id, rel) with None relation succeeds") {
      val target = PermissionTarget.unsafe("document", "123", None)
      assertTrue(target.namespace == "document" && target.value == "123" && target.rel.isEmpty)
    }
  )
end PermissionTargetSpec
