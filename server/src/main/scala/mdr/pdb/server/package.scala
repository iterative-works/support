package mdr.pdb.server

import zio.*
import org.pac4j.core.profile.CommonProfile
import mdr.pdb.server.user.UserDirectory

type AppEnv = ZEnv & UserDirectory
type AppTask = RIO[AppEnv, *]
type AppAuth = List[CommonProfile]
