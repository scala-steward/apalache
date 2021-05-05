package at.forsyte.apalache.tla.script

/**
 * Execution statistics, similar to JUnit.
 *
 * @param nSuccess the number of scripts that were executed successfully
 * @param nError   the number of scripts that produced an error
 * @param nFailed  the number of scripts that failed for various reasons (incorrect annotations, model checker failed, etc.)
 * @param nIgnored the number of scripts that were ignored due to setup options
 */
case class ScriptStatistics(nSuccess: Int, nError: Int, nFailed: Int, nIgnored: Int) {
  def add: ScriptResult => ScriptStatistics = {
    case ScriptSuccess() =>
      this.copy(nSuccess + 1)

    case ScriptError() =>
      this.copy(nError + 1)

    case ScriptFailure(_) =>
      this.copy(nFailed + 1)

    case ScriptIgnored() =>
      this.copy(nIgnored + 1)
  }
}
