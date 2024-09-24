package works.iterative.files.scenarios

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.document
import com.raquo.laminar.api.L.*
import com.raquo.laminar.shoelace.sl
import zio.json.*
import works.iterative.files.FileItem
import works.iterative.files.FileItemCodecs.given
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js

@JSExportTopLevel("scenario", "scenariofilePicker")
object FilePickerScenario:
    @JSImport("@shoelace-style/shoelace/dist/themes/light.css", "importStyle")
    @js.native
    private def importLightTheme(): Unit = js.native

    importLightTheme()

    sl.Shoelace.setBasePath("/assets/shoelace")

    @JSExport
    def main(): Unit =

        def renderFile(fileItem: FileItem.File): sl.TreeItem.Element =
            sl.TreeItem(sl.Icon(_.name("file-earmark-text")), fileItem.fileRef.name)

        def renderDirectory(directory: FileItem.Directory): sl.TreeItem.Element =
            sl.TreeItem(sl.Icon(_.name("folder")), directory.name, directory.items.map(renderItem))

        def renderItem(fileItem: FileItem) =
            fileItem match
                case fileItem: FileItem.File       => renderFile(fileItem)
                case directory: FileItem.Directory => renderDirectory(directory)

        val app = div(
            child <-- FetchStream.get("/filePicker/files.json").map: data =>
                data.fromJson[Seq[FileItem]].fold(
                    error => div("Error: ", error.toString()),
                    files =>
                        sl.Tree(
                            _.selection("leaf"),
                            files.map(renderItem)
                        )
                )
        )

        renderOnDomContentLoaded(document.getElementById("app"), app)
    end main
end FilePickerScenario
