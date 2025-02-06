package works.iterative.ui.components

object AppPageComponents:
    final case class NavLink(
        link: String,
        text: String,
        data: Map[String, String]
    )
end AppPageComponents

import AppPageComponents.*

trait AppPageComponents[T] extends Components[T]:
    /** Navigation bar component */
    def navigationBar(
        title: String,
        links: Seq[NavLink],
        userArea: T
    ): T
end AppPageComponents
