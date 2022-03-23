package ui.components.tailwind

trait TailwindSupport:
  // Make sure certain classes are included in the Tailwind JIT compiler
  // The sizes and colors that we generate dynamically are missed
  // So until I figure out how to use macros to mitigate, this should do
  val extraUserClasses: List[String] =
    List(
      "h-2",
      "h-3",
      "h-4",
      "h-5",
      "h-6",
      "h-8",
      "h-10",
      "h-12",
      "h-14",
      "h-16",
      "w-2",
      "w-3",
      "w-4",
      "w-5",
      "w-6",
      "w-8",
      "w-10",
      "w-12",
      "w-14",
      "w-16",
      "text-red-800",
      "text-amber-800",
      "text-green-800",
      "bg-red-100",
      "bg-amber-100",
      "bg-green-100"
    )
