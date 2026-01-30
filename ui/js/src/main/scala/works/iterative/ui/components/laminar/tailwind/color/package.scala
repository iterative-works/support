package works.iterative.ui.components.laminar.tailwind

/** We need a generic Color model that can be used both on server and on client.
  *
  * We have adopted the Tailwind model for now. There is nothing inherently Tailwind-specific in the
  * implementation, but all the values are taken from their palette and the area model is very HTML
  * biased.
  *
  * Still, I think that it is a good starting point for exploration and will satisfy our current
  * needs.
  */
package object color {}
