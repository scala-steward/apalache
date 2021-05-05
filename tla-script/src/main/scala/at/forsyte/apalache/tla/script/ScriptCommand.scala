package at.forsyte.apalache.tla.script

sealed abstract class ScriptCommand {
  val testName: String
}

case class StatelessScriptCommand(testName: String) extends ScriptCommand {
  val constInit: String = testName + "___cinit"
  val predicate: String = testName
}

case class ActionScriptCommand(testName: String) extends ScriptCommand {
  val constInit: String = testName + "___cinit"
  val pre: String = testName + "___init"
  val post: String = testName + "___assertion"
  val action: String = testName
}

case class ExecScriptCommand(testName: String, length: Int) extends ScriptCommand {
  val constInit: String = testName + "___cinit"
  val init: String = testName + "___init"
  val next: String = testName
  val post: String = testName + "___assertion"
  val temporalPre: String = testName + "___temporalPre"
  val temporalPost: String = testName + "___temporalPost"
}
