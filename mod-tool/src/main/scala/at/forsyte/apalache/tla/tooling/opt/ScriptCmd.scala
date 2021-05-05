package at.forsyte.apalache.tla.tooling.opt

import org.backuity.clist.{Command, _}

import java.io.File

/**
 * This command initiates the 'parse' command line.
 *
 * @author Igor Konnov
 */
class ScriptCmd
    extends Command(name = "script",
        description = "Parse the tests as in RFC006 and produce goals (provisional feature)") with General {

  var file: File = arg[File](description = "a file containing a TLA+ specification (.tla or .json)")
}
