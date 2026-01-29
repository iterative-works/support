package works.iterative.core.auth

/** Data object representing something with user information
  *
  * This is meant to be subclassed by app-specific implementations.
  */
trait UserInfo:
    def subjectId: UserId
end UserInfo
