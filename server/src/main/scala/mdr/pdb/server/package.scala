package mdr.pdb.server

import zio.*
import org.pac4j.core.profile.CommonProfile
import mdr.pdb.users.query.repo.UsersRepository
import mdr.pdb.proof.query.repo.ProofRepository
import mdr.pdb.proof.command.entity.ProofCommandBus

type AppEnv = ZEnv & UsersRepository & ProofRepository & ProofCommandBus
type AppTask = RIO[AppEnv, *]
type AppAuth = List[CommonProfile]
