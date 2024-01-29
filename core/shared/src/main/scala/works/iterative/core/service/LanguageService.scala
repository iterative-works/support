package works.iterative.core
package service

import zio.*

trait LanguageService:
    def currentLanguage: UIO[Language]
    def switchLanguage(language: Language): UIO[Unit]
