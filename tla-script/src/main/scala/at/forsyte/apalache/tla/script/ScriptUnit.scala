package at.forsyte.apalache.tla.script

trait ScriptUnit

case class StatelessScriptUnit(constInit: Option[String], predicate: String) extends ScriptUnit

case class ActionScriptUnit(constInit: Option[String], pre: String, action: String, post: String) extends ScriptUnit {}

case class ExecScriptUnit(constInit: Option[String], init: String, next: String, post: String, temporal: Option[String],
    length: Int)
    extends ScriptUnit {}
