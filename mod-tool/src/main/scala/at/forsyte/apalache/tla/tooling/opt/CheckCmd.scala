package at.forsyte.apalache.tla.tooling.opt

import java.io.File

import org.backuity.clist.{Command, _}

/**
  * This command initiates the 'check' command line.
  *
  * @author Igor Konnov
  */
class CheckCmd extends Command(name = "check",
  description = "Check a TLA+ specification") with General {

  var file: File = arg[File](description = "a file containing a TLA+ specification")
  var search: String = opt[String](
    name = "search", default = "bfs",
    description = "search type (dfs or bfs), default: bfs")
  var cinit: String = opt[String](
    name = "cinit", default = "",
    description = "the name of an operator that initializes CONSTANTS, default: None")
  var init: String = opt[String](
    name = "init", default = "Init",
    description = "the name of an operator that initializes VARIABLES, default: Init")
  var next: String = opt[String](
    name = "next", default = "Next",
    description = "the name of a transition operator, default: Next")
  var inv: String =
    opt[String](name = "inv", default = "",
      description = "the name of an invariant operator, e.g., Inv")
  var length: Int =
    opt[Int](name = "length", default = 10,
      description = "the bound on the computation length, default: 10")
  var tuning: String =
    opt[String](name="tuning", default = "",
      description = "filename of the config with file tuning options (see tuning.md)")

  var randomizeDfs: Boolean = opt[Boolean](
    name = "randomizeDfs", default = true,
    description = "randomize the choice of the next transition in DFS, default: true")
  var filter: String =
    opt[String](name = "filter", default = "",
      description = "A sequence of regular expressions over transitions that filter transitions at every step, e.g., (0|1),(1|2),4,5")
  var checkRuntime: Boolean =
    opt[Boolean](name = "checkRuntime", default = false,
      description = " (BROKEN) check for runtime errors, e.g., applying f[x] when x is outside of f's domain, default: false")
  var temporal: String = opt[String](
    name = "specification",
    default = "",
    description = "Specification of system with liveness, e.g., Temporal"
  )
  var livenessCheckModeName: String = opt[String](
    name = "liveness-check-mode",
    default = "find",
    description = "Loop search mode, e.g. enumerate, default: find"
  )
}
