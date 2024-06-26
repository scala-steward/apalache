package at.forsyte.apalache.tla.bmcmt.rules.vmt

import at.forsyte.apalache.tla.lir.TlaEx
import at.forsyte.apalache.tla.lir.formulas.Term

/**
 * ToTermRewriter defines a translation from TLA+ to SMT Terms.
 *
 * @author
 *   Jure Kukovec
 */
abstract class ToTermRewriter {
  def rewrite(ex: TlaEx): Term
}
