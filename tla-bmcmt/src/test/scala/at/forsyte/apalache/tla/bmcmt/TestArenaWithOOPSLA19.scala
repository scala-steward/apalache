package at.forsyte.apalache.tla.bmcmt

import at.forsyte.apalache.tla.bmcmt.smt.{SolverConfig, Z3SolverContext}
import org.junit.runner.RunWith
import org.scalatest.Outcome
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestArenaWithOOPSLA19 extends TestArena {
  override protected def withFixture(test: NoArgTest): Outcome = {
    solver = new Z3SolverContext(SolverConfig.default.copy(debug = true, smtEncoding = oopsla19Encoding))
    val result = test()
    solver.dispose()
    result
  }
}
