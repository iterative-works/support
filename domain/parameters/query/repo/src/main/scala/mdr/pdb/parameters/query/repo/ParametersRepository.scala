package mdr.pdb.parameters
package query
package repo

import mdr.pdb.OsobniCislo

import zio.*

object ParametersRepository:
  sealed trait Criteria:
    type Result
  trait MultiResultCriteria extends Criteria:
    override type Result = List[Parameter]
  trait SingleResultCriteria extends Criteria:
    override type Result = Option[Parameter]

  case object Any extends MultiResultCriteria
  case class WithId(id: Parameter.Id) extends SingleResultCriteria
  case class OfUser(osc: OsobniCislo) extends MultiResultCriteria

trait ParametersRepository:
  import ParametersRepository.Criteria
  def matching(criteria: Criteria): Task[criteria.Result]

private[query] trait ParametersRepositoryWrite extends ParametersRepository:
  def put(parameter: Parameter): Task[Unit]
