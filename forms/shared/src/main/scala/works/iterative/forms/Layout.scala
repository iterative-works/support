package portaly
package forms

sealed trait Layout
case class Flex(elems: List[SectionSegment]) extends Layout
case class Grid(elems: List[List[SectionSegment]]) extends Layout
