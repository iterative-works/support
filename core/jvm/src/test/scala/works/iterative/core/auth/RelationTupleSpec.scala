// PURPOSE: Test specification for RelationTuple value object
// PURPOSE: Validates creation, equality, and hashing behavior for relation tuples

package works.iterative.core.auth

import zio.test.*


object RelationTupleSpec extends ZIOSpecDefault:

  def spec = suite("RelationTupleSpec")(
    test("creates RelationTuple with valid userId, relation, and target") {
      val userId = UserId.unsafe("user123")
      val relation = "owner"
      val target = PermissionTarget.unsafe("document", "doc456")

      val tuple = RelationTuple(userId, relation, target)

      assertTrue(
        tuple.user == userId,
        tuple.relation == relation,
        tuple.target == target
      )
    },

    test("RelationTuple equality: same values should be equal") {
      val userId = UserId.unsafe("user123")
      val relation = "owner"
      val target = PermissionTarget.unsafe("document", "doc456")

      val tuple1 = RelationTuple(userId, relation, target)
      val tuple2 = RelationTuple(userId, relation, target)

      assertTrue(tuple1 == tuple2)
    },

    test("RelationTuple hashing: can be stored in Set") {
      val userId1 = UserId.unsafe("user123")
      val userId2 = UserId.unsafe("user456")
      val relation = "owner"
      val target = PermissionTarget.unsafe("document", "doc789")

      val tuple1 = RelationTuple(userId1, relation, target)
      val tuple2 = RelationTuple(userId2, relation, target)
      val tuple3 = RelationTuple(userId1, relation, target) // Duplicate of tuple1

      val tupleSet = Set(tuple1, tuple2, tuple3)

      assertTrue(
        tupleSet.size == 2,
        tupleSet.contains(tuple1),
        tupleSet.contains(tuple2)
      )
    },

    test("RelationTuple equality: different values should not be equal") {
      val userId = UserId.unsafe("user123")
      val target = PermissionTarget.unsafe("document", "doc456")

      val tuple1 = RelationTuple(userId, "owner", target)
      val tuple2 = RelationTuple(userId, "editor", target)

      assertTrue(tuple1 != tuple2)
    }
  )
end RelationTupleSpec
