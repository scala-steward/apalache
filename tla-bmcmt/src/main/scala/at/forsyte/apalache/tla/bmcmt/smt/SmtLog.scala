package at.forsyte.apalache.tla.bmcmt.smt

import at.forsyte.apalache.tla.bmcmt.ArenaCell
import at.forsyte.apalache.tla.bmcmt.smt.SmtLog.{AssertGroundExprRecord, DeclareCellRecord, DeclareInPredRecord, Record}
import at.forsyte.apalache.tla.lir.TlaEx

object SmtLog {
  /**
    * A record in the solver log
    */
  sealed abstract class Record extends Serializable

  case class DeclareCellRecord(cell: ArenaCell) extends Record with Serializable
  case class DeclareInPredRecord(set: ArenaCell, elem: ArenaCell) extends Record with Serializable
  case class AssertGroundExprRecord(ex: TlaEx) extends Record with Serializable
}

/**
  * A differential log of declarations and assertions that were submitted to the SMT solver.
  *
  * @author Igor Konnov
  */
class SmtLog(val parentLog: Option[SmtLog], val records: List[SmtLog.Record]) {

  def replay(solver: SolverContext): Unit = {
    def applyRecord: Record => Unit = {
      case DeclareCellRecord(cell) => solver.declareCell(cell)
      case DeclareInPredRecord(set, elem) => solver.declareInPredIfNeeded(set, elem)
      case AssertGroundExprRecord(ex) => solver.assertGroundExpr(ex)
    }

    // replay the parent's log first
    parentLog match {
      case Some(parent) => parent.replay(solver)
      case None => ()
    }

    // then, reply the diff
    for (record <- records) {
      applyRecord(record)
    }
  }

}
