package works.iterative.core
package service
package specs

import zio.*
import zio.test.*

abstract class KeyValueStoreSpec extends ZIOSpecDefault:
  val defaultSpec = suite("Consul spec")(
    test("get returns None if key does not exist") {
      for
        kv <- ZIO.service[StringKeyValueStore]
        uuid <- Random.nextUUID
        key = s"test/$uuid"
        value <- kv.get(key)
      yield assertTrue(value.isEmpty)
    },
    test("read/put/delete works") {
      for
        kv <- ZIO.service[StringKeyValueStore]
        uuid <- Random.nextUUID
        key = s"test/$uuid"
        originalValue <- kv.get(key)
        _ <- kv.put(key, "test_value")
        updatedValue <- kv.get(key)
        _ <- kv.remove(key)
        removedValue <- kv.get(key)
      yield assertTrue(
        originalValue.isEmpty,
        updatedValue.contains("test_value"),
        removedValue.isEmpty
      )
    }
  )
