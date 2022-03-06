package mdr.pdb.server

trait CustomTapir extends fiftyforms.tapir.Http4sCustomTapir[AppEnv]

object CustomTapir extends CustomTapir
