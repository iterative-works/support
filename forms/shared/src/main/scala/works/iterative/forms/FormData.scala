// PURPOSE: The typed value currency of form state — text or file values keyed by absolute paths
// PURPOSE: Ingests posted bodies, serves the FormState reads and carries the __items repeat convention

package portaly.forms

import works.iterative.core.FileRef
import works.iterative.ui.model.forms.{AbsolutePath, FormState, IdPath, UIFile}

enum FieldValue:
    case Text(value: String)
    case File(ref: FileRef)

final case class FormData(data: Map[AbsolutePath, List[FieldValue]]) extends FormState:
    def get(path: AbsolutePath): Option[List[FieldValue]] = data.get(path)

    override def getString(id: AbsolutePath): Option[String] =
        get(id).flatMap(_.collectFirst { case FieldValue.Text(v) => v })

    override def getInt(id: AbsolutePath): Option[Int] =
        getString(id).flatMap(_.toIntOption)

    override def getDouble(id: AbsolutePath): Option[Double] =
        getString(id).flatMap(_.toDoubleOption)

    override def getStringList(id: AbsolutePath): Option[List[String]] =
        get(id).map(_.collect { case FieldValue.Text(v) => v })

    override def getFileList(id: AbsolutePath): Option[List[UIFile]] =
        get(id).map(_.collect { case FieldValue.File(ref) => ref })

    override def itemsFor(id: AbsolutePath): List[(String, String)] =
        getStringList(id / "__items").getOrElse(Nil)
            .map(_.split(":", 2))
            .collect { case Array(item, itemType) => (item, itemType) }

    override def all(f: AbsolutePath => Boolean): List[Any] =
        data.view.filterKeys(f).values.flatten.map {
            case FieldValue.Text(v)   => v
            case FieldValue.File(ref) => ref
        }.toList

    def add(path: AbsolutePath, value: String): FormData =
        copy(data =
            data.updatedWith(path)(vs =>
                Some(vs.getOrElse(Nil) :+ FieldValue.Text(value))
            )
        )

    def filterKeys(p: AbsolutePath => Boolean): FormData =
        copy(data = data.view.filterKeys(p).toMap)

    /** Keeps this side's values on conflicting paths, adds the other side's missing ones. */
    def combineWith(other: FormData): FormData = FormData(other.data ++ data)

    /** Prefers the other side's values on conflicting paths. */
    def overrideWith(other: FormData): FormData = FormData(data ++ other.data)

    def isEmpty: Boolean = data.isEmpty
    def nonEmpty: Boolean = !isEmpty
end FormData

object FormData:
    val empty: FormData = FormData(Map.empty)

    /** Ingests a posted form body: dot-separated field names to absolute paths, text values. */
    def parse(data: Map[String, Seq[String]]): FormData =
        FormData(data.map((k, v) => IdPath.full(k) -> v.toList.map(FieldValue.Text.apply)))
end FormData
