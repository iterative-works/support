package portaly.forms
package impl

import zio.prelude.*
import zio.json.*
import works.iterative.ui.model.forms.UIFile
import works.iterative.core.FileRef
import works.iterative.ui.model.forms.FormState
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath, IdPath}

// TODO: The "Any" as value type here is clearly wrong
// For generic form, we have String or Blob (File) type
// We should handle accordingly
trait FormR extends FormState:
    def data: Map[AbsolutePath, List[Any]]
    def get(p: AbsolutePath): Option[List[Any]]
    def getId(id: String): Option[List[Any]]
    def getFirst(p: AbsolutePath): Option[Any]
    def getFirstId(id: String): Option[Any]
    def combineWith(other: FormR): FormR
    def overrideWith(other: FormR): FormR
    def under(base: RelativePath): FormR
    def add(p: RelativePath, v: String): FormR
    def all(id: AbsolutePath => Boolean): List[Any]
    def filterKeys(p: IdPath => Boolean): FormR
    def isEmpty: Boolean
    def nonEmpty: Boolean = !isEmpty

    override def getString(id: AbsolutePath): Option[String] =
        get(id).flatMap(_.collect {
            case s: String => s
        }.headOption)

    override def getInt(id: AbsolutePath): Option[Int] =
        get(id).flatMap(_.collect {
            case i: Int    => Some(i)
            case s: String => scala.util.Try(s.toInt).toOption
        }.headOption).flatten

    override def getDouble(id: AbsolutePath): Option[Double] =
        get(id).flatMap(_.collect {
            case i: Double => Some(i)
            case s: String => scala.util.Try(s.toDouble).toOption
        }.headOption).flatten

    override def getStringList(id: AbsolutePath): Option[List[String]] =
        get(id).map(_.collect {
            case s: String => s
        })

    override def getFileList(id: AbsolutePath): Option[List[UIFile]] =
        get(id).map(_.collect {
            case s: UIFile => s
        })

    override def itemsFor(id: AbsolutePath): List[(String, String)] =
        getStringList(id / "__items").map(_.map(_.split(":", 2)).collect {
            case Array(i, t) => (i, t)
        }).getOrElse(Nil)
    end itemsFor
end FormR

object FormR:
    case class Basic(base: AbsolutePath, d: Map[RelativePath, List[Any]]) extends FormR:
        override lazy val data: Map[AbsolutePath, List[Any]] = d.map((k, v) => (base / k, v))

        override lazy val isEmpty: Boolean = data.isEmpty

        override def filterKeys(p: IdPath => Boolean): FormR =
            Basic(base, d.view.filterKeys(p).toMap)

        override def all(f: AbsolutePath => Boolean): List[Any] =
            data.view.filterKeys(f).values.flatten.toList

        override def get(p: AbsolutePath): Option[List[Any]] = data.get(p)

        override def getId(id: String): Option[List[Any]] =
            get(works.iterative.ui.model.forms.IdPath.full(id))

        override def getFirst(p: AbsolutePath): Option[Any] = get(p).flatMap(_.headOption)

        override def getFirstId(id: String): Option[Any] =
            getFirst(works.iterative.ui.model.forms.IdPath.full(id))

        private def combineWithFn(other: FormR)(fn: (
            Map[RelativePath, List[Any]],
            Map[RelativePath, List[Any]]
        ) => Map[RelativePath, List[Any]]): FormR =
            def relativeTo(
                b: RelativePath,
                d: Map[RelativePath, List[Any]]
            ): Map[RelativePath, List[Any]] =
                d.map((k, v) => (b / k, v))

            def deroot(d: Map[AbsolutePath, List[Any]]): Map[RelativePath, List[Any]] =
                d.map((k, v) => (k.relativeTo(works.iterative.ui.model.forms.IdPath.Root), v))

            other match
                case Basic(b, otherD) if base == b => Basic(base, fn(d, otherD))
                case Basic(b, otherD) =>
                    val (newBase, relA, relB) = IdPath.commonPrefix(base, b)
                    Basic(newBase, fn(relativeTo(relA, d), relativeTo(relB, otherD)))
                case other => Basic(IdPath.Root, fn(deroot(data), deroot(other.data)))
            end match
        end combineWithFn

        override def combineWith(other: FormR): FormR =
            combineWithFn(other): (d1, d2) =>
                d2.foldLeft(d1) {
                    case (acc, (k, v)) =>
                        acc.updatedWith(k) {
                            case Some(v1) => Some(v1)
                            case None     => Some(v)
                        }
                }
        end combineWith

        override def overrideWith(other: FormR): FormR =
            combineWithFn(other): (d1, d2) =>
                d2.foldLeft(d1) {
                    case (acc, (k, v)) =>
                        acc.updatedWith(k) {
                            case Some(v1) => Some(v1)
                            case None     => Some(v)
                        }
                }

        override def under(p: RelativePath): FormR = Basic(base / p, d)

        override def add(p: RelativePath, v: String): FormR =
            Basic(
                base,
                d.updatedWith(p) {
                    case Some(v1) => Some(v1 :+ v)
                    case None     => Some(List(v))
                }
            )

        override def toString(): String =
            data.toSeq.map((k, v) => (k.toHtmlName -> v)).sortBy(_._1).map((k, v) =>
                s"$k -> ${v.mkString(", ")}"
            ).mkString("\n")
    end Basic

    val empty: FormR = new FormR:
        override def data: Map[AbsolutePath, List[Any]] = Map.empty

        override def filterKeys(p: IdPath => Boolean): FormR = this

        override def all(f: AbsolutePath => Boolean): List[Any] = Nil

        override val isEmpty: Boolean = true

        override def get(p: AbsolutePath): Option[List[Any]] = None

        override def getId(id: String): Option[List[Any]] = None

        override def getFirst(p: AbsolutePath): Option[Any] = None

        override def getFirstId(id: String): Option[Any] = None

        override def combineWith(other: FormR): FormR = other

        override def overrideWith(other: FormR): FormR = other

        override def under(base: RelativePath): FormR = this

        override def add(p: RelativePath, v: String): FormR =
            Basic(works.iterative.ui.model.forms.IdPath.Root, Map(p -> List(v)))

        override def toString(): String = "empty"
    end empty

    def apply(data: Map[RelativePath, List[Any]]): FormR =
        Basic(works.iterative.ui.model.forms.IdPath.Root, data)

    def fromContent(formContent: FormContent): FormR =
        def toValue(v: FormValue): Any = v match
            case FormValue.StringValue(s) => s
            case FormValue.FileValue(f)   => f
        Basic(
            works.iterative.ui.model.forms.IdPath.Root,
            formContent.value.map((k, v) => (IdPath(k.value), v.toList.map(toValue)))
        )
    end fromContent

    def data(data: (RelativePath, List[Any])*): FormR =
        Basic(works.iterative.ui.model.forms.IdPath.Root, Map(data*))

    def data(base: AbsolutePath, data: (RelativePath, List[Any])*): FormR = Basic(base, Map(data*))

    def strings(data: (String, String)*): FormR =
        Basic(
            works.iterative.ui.model.forms.IdPath.Root,
            Map(data.map((k, v) => (IdPath(k) -> List(v)))*)
        )

    def parse(data: Map[String, Seq[Any]]): FormR =
        Basic(works.iterative.ui.model.forms.IdPath.Root, data.map((k, v) => (IdPath(k), v.toList)))

    case class Builder(data: Map[RelativePath, List[Any]] = Map.empty):
        def add(p: RelativePath, v: String): Builder =
            copy(data = data.updated(p, data.getOrElse(p, Nil) :+ v))
        def set(p: RelativePath, v: String): Builder =
            copy(data = data.updated(p, List(v)))
        def add(p: RelativePath, v: List[String]): Builder =
            copy(data = data.updated(p, data.getOrElse(p, Nil) ++ v))
        def set(p: RelativePath, v: List[String]): Builder =
            copy(data = data.updated(p, v))
        def add(p: RelativePath, v: Option[String]): Builder =
            v.fold(this)(add(p, _))
        def set(p: RelativePath, v: Option[String]): Builder =
            v.fold(this)(set(p, _))
        def addAll(d: Map[String, String]): Builder =
            d.foldLeft(this)((b, i) => b.add(i._1, i._2))
        def build(root: AbsolutePath): FormR = Basic(root, data)
        def build: FormR = build(works.iterative.ui.model.forms.IdPath.Root)
    end Builder

    given identityFormR: Identity[FormR] with
        def combine(l: => FormR, r: => FormR): FormR = l.combineWith(r)
        def identity: FormR = empty
    end identityFormR

    given formREncoder: JsonEncoder[FormR] = JsonEncoder.map[String, List[String]].contramap(
        _.data.map((k, v) => (k.toHtmlName -> v.map(_.toString)))
    )

    given formRDecoder: JsonDecoder[FormR] = JsonDecoder.map[String, List[String]].map(data =>
        Basic(works.iterative.ui.model.forms.IdPath.Root, data.map((k, v) => (IdPath(k) -> v)))
    )

    given JsonCodec[FormR] = JsonCodec(formREncoder, formRDecoder)

    val jsonMediaType: String = "formr.v1+json"
end FormR
