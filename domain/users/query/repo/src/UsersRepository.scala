package mdr.pdb
package users.query
package repo

import zio.*

object UsersRepository:
  def matching(criteria: Criteria): RIO[UsersRepository, List[UserInfo]] =
    ZIO.serviceWithZIO(_.matching(criteria))

trait UsersRepository:
  def matching(criteria: Criteria): Task[List[UserInfo]]

case class MockUsersRepository(users: List[UserInfo]) extends UsersRepository:
  def matching(criteria: Criteria): Task[List[UserInfo]] =
    ZIO.succeed(
      criteria match
        case AllUsers                 => users
        case UserWithOsobniCislo(osc) => users.filter(_.personalNumber == osc)
    )

object MockUsersRepository:
  val layer: TaskLayer[UsersRepository] =
    ZLayer.fromZIO {
      val readUsers: Task[List[UserInfo]] =
        import zio.json.{*, given}
        import mdr.pdb.users.query.codecs.Codecs.given
        for
          maybeUsers <- readJsonAs(
            getClass.getResource("/users.json")
          ).runHead
          result <- ZIO
            .fromEither {
              maybeUsers match
                case None        => Left("missing users")
                case Some(users) => users.as[Map[OsobniCislo, UserInfo]]
            }
            .mapError(IllegalStateException(_))
        yield result.values.to(List)

      for {
        users <- readUsers
      } yield MockUsersRepository(users)
    }
