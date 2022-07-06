package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

trait ButtonStyles:
  def basic: String
  def primary: String
  def secondary: String
  def positive: String
  def negative: String

trait StyleGuide:
  def button: ButtonStyles
  def label: String
  def cardContent: String
  def card: String
  def input: String

object StyleGuide:
  object default extends StyleGuide:
    override val label: String =
      "text-sm font-medium text-gray-500"
    override val cardContent: String = "px-4 py-5 sm:p-6"
    override val card: String =
      "bg-white shadow sm:rounded-md overflow-hidden"
    override val input: String =
      "shadow-sm block focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md"

    override object button extends ButtonStyles:
      private def common(extra: String) =
        s"inline-flex items-center px-4 py-2 $extra border rounded-md shadow-sm text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-75"
      override val basic: String =
        common(
          "border-gray-300 text-gray-700 bg-white enabled:hover:bg-gray-50"
        )
      override val primary: String =
        common(
          "border-transparent text-white bg-indigo-600 enabled:hover:bg-indigo-700"
        )
      override val secondary: String =
        common(
          "border-gray-300 text-indigo-700 bg-indigo-100 enabled:hover:bg-indigo-200"
        )
      override val positive: String =
        common(
          "border-gray-300 text-white bg-green-600 enabled:hover:bg-green-700"
        )
      override val negative: String =
        common("border-gray-300 text-white bg-red-600 enabled:hover:bg-red-700")
