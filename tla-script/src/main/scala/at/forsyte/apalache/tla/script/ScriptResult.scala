package at.forsyte.apalache.tla.script

sealed trait ScriptResult {}

case class ScriptSuccess() extends ScriptResult

case class ScriptError() extends ScriptResult

case class ScriptFailure(message: String) extends ScriptResult

case class ScriptIgnored() extends ScriptResult
