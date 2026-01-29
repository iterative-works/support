package works.iterative.akka

import akka.serialization.*
import zio.json.*
import scala.reflect.ClassTag

// scalafix:off DisableSyntax.throw
// Akka serialization API requires exceptions for serialization failures
// Use manifest to ensure the possibility of schema evolution
class AkkaZioJsonSerializer[T <: AnyRef](
    override val identifier: Int,
    manifestDiscriminator: T => String
)(using JsonCodec[T])(using ct: ClassTag[T])
    extends SerializerWithStringManifest:

    override def manifest(o: AnyRef): String =
        o match
        case p: T => manifestDiscriminator(p)
        case _ =>
            throw IllegalArgumentException(
                s"Invalid object to serialize, expecting ${ct.runtimeClass
                        .getSimpleName()}, got: ${o}"
            )

    override def toBinary(o: AnyRef): Array[Byte] =
        o match
        case p: T => p.toJson.getBytes("UTF-8")
        case _ =>
            throw IllegalArgumentException(
                s"Invalid object to serialize, expecting ${ct.runtimeClass
                        .getSimpleName()}, got: ${o}"
            )

    override def fromBinary(o: Array[Byte], manifest: String): AnyRef =
        val json = String(o, "UTF-8")
        json.fromJson[T] match
        case Left(t) =>
            throw IllegalStateException(
                s"Unable to deserialize ${ct.runtimeClass.getSimpleName()} from $json: $t"
            )
        case Right(e) => e
        end match
    end fromBinary
end AkkaZioJsonSerializer
// scalafix:on DisableSyntax.throw
