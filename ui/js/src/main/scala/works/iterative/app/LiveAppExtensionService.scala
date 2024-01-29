package works.iterative.app

import zio.*
import com.raquo.laminar.api.L.*

class LiveAppExtensionService(ref: Ref[Vector[AppExtension]])
    extends AppExtensionService:
    override def registerExtension(extension: AppExtension): UIO[Unit] =
        ref.update(_ :+ extension)

    override def hookAppView(appView: HtmlElement): UIO[HtmlElement] =
        for
            extensions <- ref.get
            view <- ZIO.foldLeft(extensions)(appView)((acc, ext) =>
                ext.appViewHook(acc)
            )
        yield view
end LiveAppExtensionService

object LiveAppExtensionService:
    given ZLayer.Derive.Default.WithContext[Any, Nothing, Vector[AppExtension]] =
        ZLayer.Derive.Default.succeed(Vector.empty)

    val layer: ULayer[AppExtensionService] =
        ZLayer.derive[LiveAppExtensionService]
end LiveAppExtensionService
