package mdr.pdb.server

import org.http4s.dsl.Http4sDsl

trait CustomDsl extends Http4sDsl[AppTask]
