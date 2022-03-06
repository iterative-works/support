package mdr.pdb

opaque type OsobniCislo = String

object OsobniCislo:
  // TODO: validation
  def apply(osc: String): OsobniCislo = osc

  extension (osc: OsobniCislo) def toString: String = osc
