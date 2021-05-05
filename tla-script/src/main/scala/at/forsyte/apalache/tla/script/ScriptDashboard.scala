package at.forsyte.apalache.tla.script

import at.forsyte.apalache.tla.lir.TlaModule

/**
 * ScriptBoard collects all scripts to be executed as well as modules the scripts should be executed against.
 * It also collects execution statistics.
 *
 * @author Igor Konnov
 */
class ScriptDashboard {
  private var _stats: ScriptStatistics = ScriptStatistics(nSuccess = 0, nError = 0, nFailed = 0, nIgnored = 0)

  /**
   * Get the execution statistics.
   */
  val stats: ScriptStatistics = _stats

  private var queue: List[(TlaModule, ScriptCommand)] = List.empty
  private var executed: List[(ScriptCommand, ScriptResult)] = List.empty

  /**
   * Push a script command in the execution queue.
   *
   * @param module  a module to execute the script against
   * @param command a command
   */
  def push(module: TlaModule, command: ScriptCommand): Unit = {
    queue = queue :+ (module, command)
  }

  /**
   * Get the length of the execution queue.
   *
   * @return a non-negative queue length
   */
  def queueLen: Int = queue.length

  /**
   * If the queue is not empty, remove the first module and command for execution. If the queue is empty, return None.
   *
   * @return Some((module, command)), if the queue is non-empty; otherwise, None.
   */
  def pop(): Option[(TlaModule, ScriptCommand)] = {
    queue match {
      case Nil => None
      case head :: tail =>
        queue = tail
        Some(head)
    }
  }

  /**
   * Save the result of a command execution.
   *
   * @param command the command that was executed
   * @param result  the execution result
   */
  def addCommandResult(command: ScriptCommand, result: ScriptResult): Unit = {
    executed = executed :+ (command, result)
    _stats = _stats.add(result)
  }

  /**
   * Add a result without providing a command. This can be useful when a test annotation could not be parsed,
   * but we still want to report a failed test.
   */
  def addResultWithoutCommand(result: ScriptResult): Unit = {
    _stats = _stats.add(result)
  }
}
