package works.iterative.ui.model.forms

import works.iterative.core.MessageId
import works.iterative.core.MessageCatalogue
import scala.annotation.tailrec

sealed trait IdPath:
    type NestType <: IdPath
    def path: Vector[String]
    def up: NestType
    def /(id: String): NestType
    def /(id: RelativePath): NestType

    def last: String = path.lastOption.getOrElse("")

    def size: Int = path.size

    def segments: Seq[String] = path

    def startsWith(other: IdPath): Boolean = path.startsWith(other.path)

    def serialize: String = path.mkString(".")

    def toHtmlId: String = path.mkString("-")

    def toMessageId: MessageId = MessageId(path.mkString("."))

    def toHtmlName: String = path.mkString(".")

    def toMessageId(itemType: String): MessageId = MessageId(s"${path.mkString(".")}.${itemType}")

    def toMessageIds(itemType: String): Seq[MessageId] =
        path.scanRight(itemType)(_ + "." + _).map(MessageId(_)).init

    def toMessage(itemType: String)(using messages: MessageCatalogue): String =
        toMessageIds(itemType) match
            case Vector[MessageId](h) => messages(h)
            case h +: hs              => messages(h, hs*)

    def toMessageOpt(itemType: String)(using
        messages: MessageCatalogue
    ): Option[String] =
        toMessageIds(itemType) match
            case Vector[MessageId](h) => messages.get(h)
            case h +: hs              => messages.opt(h, hs*)
end IdPath

sealed trait AbsolutePath extends IdPath:
    override type NestType = AbsolutePath
    def relativeTo(base: AbsolutePath): RelativePath =
        if base == IdPath.Root then IdPath.RelPath(path)
        else
            assume(startsWith(base))
            IdPath.RelPath(path.drop(base.size))
end AbsolutePath

sealed trait RelativePath extends IdPath:
    override type NestType = RelativePath

object IdPath:
    case object Root extends AbsolutePath:
        override val path: Vector[String] = Vector.empty
        def up: AbsolutePath = this
        def /(p: String): AbsolutePath = FullPath(Vector(p))
        def /(p: RelativePath): AbsolutePath = FullPath(p.path)
    end Root

    case object Empty extends RelativePath:
        override val path: Vector[String] = Vector.empty

        def up: RelativePath = this
        def /(p: String): RelativePath = RelPath(Vector(p))
        def /(p: RelativePath): RelativePath = p
    end Empty

    case class FullPath(path: Vector[String]) extends AbsolutePath:
        def up: AbsolutePath = if path.size == 1 then Root else FullPath(path.init)
        def /(p: String): AbsolutePath = FullPath(path :+ p)
        def /(p: RelativePath): AbsolutePath = FullPath(path ++ p.path)
    end FullPath

    case class RelPath(path: Vector[String]) extends RelativePath:
        def up: RelativePath = if path.size == 1 then Empty else RelPath(path.init)
        def /(p: String): RelativePath = RelPath(path :+ p)
        def /(p: RelativePath): RelativePath = RelPath(path ++ p.path)
    end RelPath

    def commonPrefix(a: AbsolutePath, b: AbsolutePath): (AbsolutePath, RelativePath, RelativePath) =
        if a.startsWith(b) then (b, a.relativeTo(b), IdPath.Empty)
        else if b.startsWith(a) then (a, IdPath.Empty, b.relativeTo(a))
        else
            // Find a common prefix of a.path and b.path, if any
            val common = a.path.zip(b.path).takeWhile((a, b) => a == b).map(_._1)
            if common.isEmpty then (IdPath.Root, RelPath(a.path), RelPath(b.path))
            else
                val commonBase = FullPath(common)
                (commonBase, a.relativeTo(commonBase), b.relativeTo(commonBase))
    end commonPrefix

    def parse(idPath: String, base: AbsolutePath = IdPath.Root): AbsolutePath =
        if idPath.startsWith(".") then full(idPath.substring(1))
        else if idPath.startsWith("^") then
            @tailrec
            def upOrApply(v: Vector[String], b: AbsolutePath): AbsolutePath =
                if v.isEmpty then b
                else if v.head == "^" then upOrApply(v.tail, b.up)
                else upOrApply(v.tail, b / v.head)
            upOrApply(idPath.split("\\.").toVector, base)
        else base / apply(idPath)

    def parseHtmlId(idPath: String): AbsolutePath =
        FullPath(idPath.split("-").toVector)

    def apply(id: String): RelativePath = RelPath(id.split("\\.").toVector)

    def full(id: String): AbsolutePath = FullPath(id.split("\\.").toVector)

    def unapplySeq(idPath: IdPath): Option[Seq[String]] = Some(idPath.path)

    given Conversion[String, RelativePath] = apply(_)
end IdPath
