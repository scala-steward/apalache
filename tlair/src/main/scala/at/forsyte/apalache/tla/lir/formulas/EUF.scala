package at.forsyte.apalache.tla.lir.formulas

/**
 * EUF defines constructors for terms in the fragment of (E)quality and (U)ninterpreted (f)unctions.
 *
 * @author
 *   Jure Kukovec
 */
object EUF {

  import Booleans.BoolExpr

  trait FnExpr extends Term {
    override val sort: FunctionSort
  }

  sealed case class UninterpretedLiteral(s: String, sort: UninterpretedSort) extends Term
  sealed case class UninterpretedVar(name: String, sort: UninterpretedSort) extends Variable(name)
  sealed case class Equal(lhs: Term, rhs: Term) extends BoolExpr {
    // Sanity check
    require(lhs.sort == rhs.sort, "Equality is only defined for terms of matching sorts.")
  }
  sealed case class ITE(cond: Term, thenTerm: Term, elseTerm: Term) extends Term {
    // Sanity check
    require(cond.sort == BoolSort(), "IF-condition must have Boolean sort.")
    require(thenTerm.sort == elseTerm.sort, "ITE is only defined for branches of matching sorts.")
    val sort: Sort = thenTerm.sort
  }

  /**
   * A function term. FunDef plays a dual role, because it conceptually represents side-effects: SMT requires that each
   * function is defined separately from where it is used, unlike TLA. If we want to translate a TLA syntax-tree to
   * s-expressions, we either need side-effects (for introducing definitions), or as is the case with FunDef, we pack
   * the definition with the term, and recover it later (see VMTWriter::Collector)
   *
   * In terms of s-expressions (and when translated to a string), it is equivalent to FunctionVar(name, sort).
   */
  sealed case class FunDef(name: String, args: List[(String, Sort)], body: Term) extends FnExpr {
    val sort: FunctionSort = FunctionSort(body.sort, args.map { _._2 }: _*)
  }
  sealed case class FunctionVar(name: String, sort: FunctionSort) extends Variable(name) with FnExpr
  sealed case class Apply(fn: Term, args: Term*) extends Term {
    require(hasFnSort(fn), "Apply is only defined for terms with function sorts.")
    private val asFnSort = fn.sort.asInstanceOf[FunctionSort]

    // Apply is valid, if fn has a function sort (S1, ..., Sn) -> S
    // and args have sorts S1, ..., Sn. Then, Apply has sort S
    require(isValid, "Apply is only defined when the sorts of the arguments fit the function's FunctionSort")
    private def isValid: Boolean =
      if (args.size != asFnSort.from.size) false
      else {
        args.zip(asFnSort.from).forall { case (arg, expectedSort) =>
          arg.sort == expectedSort
        }
      }

    // True iff term has a function sort
    private def hasFnSort(term: Term) = term.sort match {
      case _: FunctionSort => true
      case _               => false
    }
    val sort: Sort = asFnSort.to
  }
}
