package works.iterative.app

import zio.*
import com.raquo.laminar.api.L.*

trait AppExtension:
    def appViewHook(appView: HtmlElement): UIO[HtmlElement] = ZIO.succeed(appView)

trait AppExtensionService:
    def registerExtension(extension: AppExtension): UIO[Unit]
    def hookAppView(appView: HtmlElement): UIO[HtmlElement]
