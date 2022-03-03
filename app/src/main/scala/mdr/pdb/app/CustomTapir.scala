package mdr.pdb.app

import sttp.tapir.client.sttp.SttpClientInterpreter

trait CustomTapir extends mdr.pdb.api.CustomTapir with SttpClientInterpreter

object CustomTapir extends CustomTapir
