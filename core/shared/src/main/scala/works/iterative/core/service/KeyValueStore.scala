package works.iterative.core.service

import zio.*
import zio.json.*

trait GenericReadKeyValueStore[Eff[+_], -Key, +Value]:
  type Op[A] = Eff[A]
  def get(key: Key): Eff[Option[Value]]

trait GenericWriteKeyValueStore[Eff[+_], -Key, -Value]:
  def put(key: Key, value: Value): Eff[Unit]

trait GenericKeyValueStore[Eff[+_], -Key, Value]
    extends GenericReadKeyValueStore[Eff, Key, Value]
    with GenericWriteKeyValueStore[Eff, Key, Value]

trait ReadKeyValueStore[-Key, +Value]
    extends GenericReadKeyValueStore[UIO, Key, Value]

trait WriteKeyValueStore[-Key, -Value]
    extends GenericWriteKeyValueStore[UIO, Key, Value]

trait KeyValueStore[-Key, Value]
    extends GenericKeyValueStore[UIO, Key, Value]
    with ReadKeyValueStore[Key, Value]
    with WriteKeyValueStore[Key, Value]

type StringKeyValueStore = KeyValueStore[String, String]

object KeyValueStore:
  extension (store: StringKeyValueStore)
    /** Decode the value, ignoring decoding errors if any */
    def getAsMaybe[A: JsonDecoder](key: String): UIO[Option[A]] =
      store.get(key).map(_.flatMap(_.fromJson[A].toOption))

    def getAsMaybeLogged[A: JsonDecoder](key: String): UIO[Option[A]] =
      store.get(key).flatMap {
        case None => ZIO.none
        case Some(v) =>
          ZIO.fromEither(v.fromJson[A]).asSome.catchAll { err =>
            ZIO.log(err) *> ZIO.none
          }
      }

    def putAsJson[A: JsonEncoder](key: String, value: A): UIO[Unit] =
      store.put(key, value.toJson)
