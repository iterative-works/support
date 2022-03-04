package mdr.pdb
package server
package user

import zio.*

trait UserDirectory:
  def list: Task[List[UserInfo]]

object UserDirectory:
  def list: RIO[UserDirectory, List[UserInfo]] = ZIO.serviceWithZIO(_.list)

case class MockUserDirectory(users: List[UserInfo]) extends UserDirectory:
  def list: Task[List[UserInfo]] = ZIO.succeed(users)

object MockUserDirectory:
  val layer: TaskLayer[UserDirectory] =
    ZLayer.fromZIO {
      val readUsers: Task[List[UserInfo]] =
        import zio.json.{*, given}
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
      } yield MockUserDirectory(users)
    }
