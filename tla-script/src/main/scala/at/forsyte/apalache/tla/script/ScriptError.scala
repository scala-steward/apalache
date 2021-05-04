package at.forsyte.apalache.tla.script

/**
 * An error that is found in a script.
 *
 * @param message error message
 */
case class ScriptError(test: String, message: String) {}
