package mdr.pdb.server

import zio.*

trait HttpServer:
  def serve(): URIO[AppEnv, ExitCode]
