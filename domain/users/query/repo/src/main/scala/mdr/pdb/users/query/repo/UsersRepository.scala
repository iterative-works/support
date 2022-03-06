package mdr.pdb
package users.query
package repo

import zio.*

trait UsersRepository:
  def list: Task[List[UserInfo]]

object UsersRepository:
  def list: RIO[UsersRepository, List[UserInfo]] = ZIO.serviceWithZIO(_.list)

case class MockUsersRepository(users: List[UserInfo]) extends UsersRepository:
  def list: Task[List[UserInfo]] = ZIO.succeed(users)

object MockUsersRepository:
  val layer: TaskLayer[UsersRepository] =
    ZLayer.fromZIO {
      val readUsers: Task[List[UserInfo]] =
        import zio.json.{*, given}
        import mdr.pdb.users.query.json.Codecs.given
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
