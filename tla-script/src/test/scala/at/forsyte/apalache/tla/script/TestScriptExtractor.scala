package at.forsyte.apalache.tla.script

import at.forsyte.apalache.tla.lir.{BoolT1, TlaModule, TlaOperDecl}
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.transformations.impl.IdleTracker
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import at.forsyte.apalache.tla.lir.TypedPredefs._

@RunWith(classOf[JUnitRunner])
class TestScriptExtractor extends FunSuite with BeforeAndAfter with ModuleFixture {
  test("extract TestStateless") {
    val store = mkAnnotationStore()
    val extractor = new ScriptExtractor(store, new IdleTracker())
    val input = fixtureModule.copy(declarations = fixtureModule.declarations :+ testStateless)
    val dashboard = extractor(input)
    assert(0 == dashboard.stats.nIgnored)
    assert(0 == dashboard.stats.nFailed)
    assert(0 == dashboard.stats.nSuccess)
    assert(0 == dashboard.stats.nError)
    assert(1 == dashboard.queueLen)
    dashboard.pop() match {
      case None => fail("Expected a test")

      case Some((module, cmd @ StatelessScriptCommand(testName))) =>
        assert("TestStateless" == testName)
        assertOperator(module, cmd.constInit) { oper =>
          // the constant initializer is: ConstInit
          assert(constInit.body == oper.body)
        }
        assertOperator(module, cmd.predicate) { oper =>
          // the assertion is: Assertion
          assert(testStateless.body == oper.body)
        }
    }
  }

  test("extract TestAction") {
    val store = mkAnnotationStore()
    val extractor = new ScriptExtractor(store, new IdleTracker())
    val input = fixtureModule.copy(declarations = fixtureModule.declarations :+ testAction)
    val dashboard = extractor(input)
    assert(0 == dashboard.stats.nIgnored)
    assert(0 == dashboard.stats.nFailed)
    assert(0 == dashboard.stats.nSuccess)
    assert(0 == dashboard.stats.nError)
    assert(1 == dashboard.queueLen)
    dashboard.pop() match {
      case None => fail("Expected a test")

      case Some((module, cmd @ ActionScriptCommand(testName))) =>
        assert("TestAction" == testName)
        assertOperator(module, cmd.constInit) { oper =>
          // the constant initializer is: ConstInit /\ Assumptions
          assert(tla.and(constInit.body, assumptions.body).typed(BoolT1()) == oper.body)
        }
        assertOperator(module, cmd.pre) { oper =>
          // the state initializer is: TypeOK
          assert(typeOk.body == oper.body)
        }
        assertOperator(module, cmd.post) { oper =>
          // the assertion is: Assertion
          assert(assertion.body == oper.body)
        }
        assertOperator(module, cmd.action) { oper =>
          // the assertion is: TestAction
          assert(testAction.body == oper.body)
        }
    }
  }

  test("extract TestExecution") {
    val store = mkAnnotationStore()
    val extractor = new ScriptExtractor(store, new IdleTracker())
    val input = fixtureModule.copy(declarations = fixtureModule.declarations :+ testExec)
    val dashboard = extractor(input)
    assert(0 == dashboard.stats.nIgnored)
    assert(0 == dashboard.stats.nFailed)
    assert(0 == dashboard.stats.nSuccess)
    assert(0 == dashboard.stats.nError)
    assert(1 == dashboard.queueLen)
    dashboard.pop() match {
      case None => fail("Expected a test")

      case Some((module, cmd @ ExecScriptCommand(testName, 5))) =>
        assert("TestExecution" == testName)
        assertOperator(module, cmd.constInit) { oper =>
          // the constant initializer is: ConstInit /\ Assumptions
          assert(tla.and(constInit.body, assumptions.body).typed(BoolT1()) == oper.body)
        }
        assertOperator(module, cmd.init) { oper =>
          // the state initializer is: TypeOK
          assert(typeOk.body == oper.body)
        }
        assertOperator(module, cmd.post) { oper =>
          // the assertion is: Assertion
          assert(assertion.body == oper.body)
        }
        assertOperator(module, cmd.temporalPre) { oper =>
          // the assertion is: Live
          assert(live.body == oper.body)
        }
        assertOperator(module, cmd.temporalPost) { oper =>
          // the assertion is: Safe
          assert(safe.body == oper.body)
        }
        assertOperator(module, cmd.next) { oper =>
          // the assertion is: TestExecution
          assert(testExec.body == oper.body)
        }
    }
  }

  private def assertOperator(module: TlaModule, name: String)(pred: TlaOperDecl => Unit): Unit = {
    module.operDeclarations.find(_.name == name) match {
      case Some(oper) =>
        pred(oper)

      case None =>
        fail(s"$name not found in module ${module.name}")
    }
  }
}
