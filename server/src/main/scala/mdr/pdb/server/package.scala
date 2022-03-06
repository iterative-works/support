package mdr.pdb.server

import zio.*
import org.pac4j.core.profile.CommonProfile
import mdr.pdb.users.query.repo.UsersRepository

type AppEnv = ZEnv & UsersRepository
type AppTask = RIO[AppEnv, *]
type AppAuth = List[CommonProfile]
