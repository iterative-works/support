package works.iterative.files

import works.iterative.core.FileRef

sealed trait FileItem:
    def name: String
    def allEntries: List[FileItem]
    def allFiles: List[(List[String], FileItem.File)]
end FileItem

object FileItem:
    case class File(fileRef: FileRef) extends FileItem:
        def name: String = fileRef.name
        def allEntries: List[FileItem] = List(this)
        def allFiles: List[(List[String], FileItem.File)] = List(Nil -> this)

    case class Directory(name: String, items: List[FileItem]) extends FileItem:
        def allEntries: List[FileItem] = this :: items.flatMap(_.allEntries)
        def allFiles: List[(List[String], FileItem.File)] =
            items.flatMap(_.allFiles).map((p, f) => (name :: p, f))

        def add(path: List[String], item: FileItem): Directory =
            path match
                case Nil          => copy(items = items :+ item)
                case head :: tail =>
                    // Try to find nested directory
                    val child: Directory = items.collectFirst {
                        case d @ Directory(name, _) if name == head => d
                    }.getOrElse(Directory(head, Nil))

                    // Replace the dir with new content
                    copy(items = items.filterNot { _ == child } :+ child.add(tail, item))
    end Directory

    def Root: Directory = Directory("", Nil)
end FileItem
