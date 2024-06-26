package at.forsyte.apalache.tla.bmcmt.rules

import at.forsyte.apalache.tla.bmcmt._
import at.forsyte.apalache.tla.bmcmt.types.CellTFrom
import at.forsyte.apalache.tla.lir.UntypedPredefs._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.oper.TlaSetOper
import at.forsyte.apalache.tla.lir.{OperEx, SetT1, TypingException}

/**
 * Implements the rule for a union of all set elements, that is, UNION S for a set S that contains sets as elements.
 *
 * @author
 *   Igor Konnov
 */
class SetUnionRule(rewriter: SymbStateRewriter) extends RewritingRule {

  override def isApplicable(symbState: SymbState): Boolean = {
    symbState.ex match {
      case OperEx(TlaSetOper.union, _) => true
      case _                           => false
    }
  }

  override def apply(state: SymbState): SymbState = {
    state.ex match {
      case OperEx(TlaSetOper.union, topSet) =>
        // rewrite the arguments into memory cells
        var nextState = rewriter.rewriteUntilDone(state.setRex(topSet))
        val topSetCell = nextState.asCell
        val elemType =
          topSetCell.cellType match {
            case CellTFrom(SetT1(SetT1(et))) =>
              et

            case _ =>
              throw new TypingException(s"Applying UNION to $topSet of type ${topSetCell.cellType}", state.ex.ID)
          }

        val sets = Set(nextState.arena.getHas(topSetCell): _*).toList // remove duplicates too
        val elemsOfSets = sets.map(s => Set(nextState.arena.getHas(s): _*))

        val unionOfSets = elemsOfSets.foldLeft(Set[ArenaCell]())(_.union(_))
        // introduce a set cell
        nextState = nextState.updateArena(_.appendCell(SetT1(elemType)))
        val newSetCell = nextState.arena.topCell

        if (unionOfSets.isEmpty) {
          // just return an empty set
          // TODO: cache empty sets!
          nextState.setRex(newSetCell.toNameEx)
        } else {
          // add all the elements to the arena
          nextState = nextState.updateArena(_.appendHas(newSetCell, unionOfSets.toSeq: _*))

          // Require each cell to be in one of the sets, e.g., consider UNION { {1} \ {1}, {1} }
          // Importantly, when elemCell is pointed by several sets S_1, .., S_k, we require:
          //    in(elemCell, newSetCell)
          //      <=> in(elemCell, S_1) /\ in(S_1, topSetCell) \/ ... \/ in(elemCell, S_k) /\ in(S_k, topSetCell)
          //
          // This approach is more expensive at the rewriting phase, but it produces O(n) constraints in SMT,
          // in contrast to the old approach with equalities and uninterpreted functions, which required O(n^2) constraints.
          def addOneElemCons(elemCell: ArenaCell): Unit = {
            def isPointedBySet(setElems: Set[ArenaCell]): Boolean = setElems.contains(elemCell)
            def inPointingSet(set: ArenaCell) = {
              // this is sound, because we have generated element equalities
              // and thus can use congruence of in(...) for free
              tla.and(tla.apalacheSelectInSet(set.toNameEx, topSetCell.toNameEx),
                  tla.apalacheSelectInSet(elemCell.toNameEx, set.toNameEx))
            }

            val pointingSets = sets.zip(elemsOfSets).collect { case (set, elems) if isPointedBySet(elems) => set }
            assert(pointingSets.nonEmpty)
            val existsIncludingSet = tla.or(pointingSets.map(inPointingSet): _*)
            val inUnionSet = tla.apalacheStoreInSet(elemCell.toNameEx, newSetCell.toNameEx)
            val notInUnionSet = tla.apalacheStoreNotInSet(elemCell.toNameEx, newSetCell.toNameEx)
            val ite = tla.ite(existsIncludingSet, inUnionSet, notInUnionSet)
            rewriter.solverContext.assertGroundExpr(ite)
          }

          // add SMT constraints
          unionOfSets.foreach(addOneElemCons)

          // that's it
          nextState.setRex(newSetCell.toNameEx)
        }

      case _ =>
        throw new RewriterException("%s is not applicable".format(getClass.getSimpleName), state.ex)
    }
  }
}
