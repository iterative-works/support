package mdr.pdb.server

trait CustomTapir extends works.iterative.tapir.Http4sCustomTapir[AppEnv]

object CustomTapir extends CustomTapir
