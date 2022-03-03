package mdr.pdb.server

import zio.*

trait HttpServer:
  def serve(): UIO[ExitCode]
