package mdr.pdb.parameters
package query
package repo

import mdr.pdb.OsobniCislo

import zio.*

trait ParametersRepository:
  def allParameters(): Task[List[Parameter]]
  def parametersOfUser(user: OsobniCislo): Task[List[Parameter]]
  def proofsOfUser(user: OsobniCislo): Task[List[Proof]]
