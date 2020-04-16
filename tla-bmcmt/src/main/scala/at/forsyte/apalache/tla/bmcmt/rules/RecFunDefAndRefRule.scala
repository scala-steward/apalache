package at.forsyte.apalache.tla.bmcmt.rules

import at.forsyte.apalache.tla.bmcmt._
import at.forsyte.apalache.tla.bmcmt.implicitConversions._
import at.forsyte.apalache.tla.bmcmt.rules.aux.CherryPick
import at.forsyte.apalache.tla.bmcmt.types._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.oper.TlaFunOper
import at.forsyte.apalache.tla.lir.values.TlaIntSet
import at.forsyte.apalache.tla.lir.{NameEx, OperEx, TlaEx, ValEx}

/**
  * This rule translates the definition of a recursive function. It is similar to CHOOSE.
  * Unlike CHOOSE, it does not have a fallback option, when the definition has no solution.
  * Hence, an infeasible definition of a recursive function may lead to a deadlock.
  *
  * @author Igor Konnov
  */
class RecFunDefAndRefRule(rewriter: SymbStateRewriter) extends RewritingRule {
  private val pick = new CherryPick(rewriter)

  override def isApplicable(symbState: SymbState): Boolean = {
    symbState.ex match {
      case OperEx(TlaFunOper.recFunDef, _*) => true
      case OperEx(TlaFunOper.recFunRef) => true
      case _ => false
    }
  }

  override def apply(state: SymbState): SymbState = {
    state.ex match {
      case OperEx(TlaFunOper.recFunDef, mapEx, NameEx(varName), setEx) =>
        rewriteFunCtor(state, mapEx, varName, setEx)

      case OperEx(TlaFunOper.recFunRef) =>
        val name = TlaFunOper.recFunRef.uniqueName
        if (state.binding.contains(name)) {
          state.setRex(state.binding(name)).setTheory(CellTheory())
        } else {
          throw new RewriterException("Referencing a recursive function that is undefined", state.ex)
        }

      case _ =>
        throw new RewriterException("%s is not applicable to %s"
          .format(getClass.getSimpleName, state.ex), state.ex)
    }
  }

  private def rewriteFunCtor(state: SymbState, mapEx: TlaEx, varName: String, setEx: TlaEx) = {
    // rewrite the set expression into a memory cell
    var nextState = rewriter.rewriteUntilDone(state.setTheory(CellTheory()).setRex(setEx))
    val domainCell = nextState.asCell
    val elemT = domainCell.cellType match {
      case FinSetT(et) => et
      case t@_ => throw new RewriterException("Expected a finite set, found: " + t, state.ex)
    }
    // find the type of the target expression and of the target set
    val resultT = rewriter.typeFinder.computeRec(mapEx)
    val codomain =
      resultT match {
        case IntT() => ValEx(TlaIntSet)
        case BoolT() => tla.booleanSet()
        case _ =>
          val msg = "A result of a recursive function must belong to Int or BOOLEAN. Found: " + resultT
          throw new RewriterException(msg, state.ex)
      }

    val funT =
      rewriter.typeFinder
        .compute(state.ex, resultT, elemT, domainCell.cellType)
        .asInstanceOf[FunT]

    // produce a cell for the function set (no expansion happens there)
    nextState = rewriter.rewriteUntilDone(nextState.setRex(tla.funSet(domainCell, codomain)))
    val funSetCell = nextState.asCell
    // pick a function from the function set
    nextState = pick.pickFunFromFunSet(funT, funSetCell, nextState)
    val funCell = nextState.asCell
    nextState = nextState.setBinding(nextState.binding + (TlaFunOper.recFunRef.uniqueName -> funCell))
    // for every element of the domain, add the constraint imposed by the definition
    val domainCells = nextState.arena.getHas(domainCell)
    for (elem <- domainCells) {
      val oldBinding = nextState.binding
      // compute the right-hand side of the constraint by the recursive function
      nextState = rewriter.rewriteUntilDone(nextState.setRex(mapEx).setBinding(oldBinding + (varName -> elem)))
      val rhs = nextState.asCell
      // Compute the left-hand side of the constraint, that is, f[elem].
      nextState = nextState.setBinding(oldBinding)
      nextState = rewriter.rewriteUntilDone(nextState.setRex(tla.appFun(funCell, elem)))
      val lhs = nextState.asCell
      // either elem is outside of DOMAIN, or lhs equals rhs
      val pred = tla.or(tla.not(tla.in(elem, domainCell)), tla.eql(lhs, rhs))
      rewriter.solverContext.assertGroundExpr(pred)
    }

    // that's it
    val finalState = nextState.setRex(funCell.toNameEx)
      .setBinding(nextState.binding - TlaFunOper.recFunRef.uniqueName)
      .setTheory(CellTheory())
    rewriter.coerce(finalState, state.theory)
  }
}
