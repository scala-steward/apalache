package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.{Annotation, AnnotationIdent, AnnotationInt, AnnotationStr}
import at.forsyte.apalache.io.annotations.store.{AnnotationStore, createAnnotationStore}
import at.forsyte.apalache.tla.lir.UntypedPredefs._
import at.forsyte.apalache.tla.lir.{TlaConstDecl, TlaModule, TlaOperDecl, TlaVarDecl}
import at.forsyte.apalache.tla.lir.convenience.tla._

trait ModuleFixture {
  protected val constN: TlaConstDecl = TlaConstDecl("N")
  protected val varX: TlaVarDecl = TlaVarDecl("x")
  protected val constInit: TlaOperDecl = TlaOperDecl("ConstInit", List(), eql(name("N"), int(10)))
  protected val assumptions: TlaOperDecl = TlaOperDecl("Assumptions", List(), lt(name("N"), int(5)))
  protected val typeOk: TlaOperDecl =
    TlaOperDecl("TypeOK", List(), in(name("x"), dotdot(int(1), name("N"))))
  protected val assertion: TlaOperDecl =
    TlaOperDecl("Assertion", List(), lt(prime(name("x")), name("x")))
  protected val safe: TlaOperDecl = TlaOperDecl("Safe", List(), box(eql(name("x"), int(2))))
  protected val live: TlaOperDecl = TlaOperDecl("Live", List(), diamond(eql(name("x"), int(2))))
  protected val testAction: TlaOperDecl = TlaOperDecl("TestAction", List(), eql(prime(name("x")), name("x")))
  protected val testStateless: TlaOperDecl = TlaOperDecl("TestStateless", List(), eql(int(2), plus(int(1), int(1))))
  protected val testExec: TlaOperDecl = TlaOperDecl("TestExecution", List(), eql(prime(name("x")), name("x")))

  protected val fixtureModule: TlaModule =
    TlaModule("root", Seq(constN, varX, constInit, assumptions, typeOk, assertion, safe, live))

  def mkAnnotationStore(): AnnotationStore = {
    val store = createAnnotationStore()
    val requireCInit = Annotation("require", AnnotationIdent("ConstInit"))
    val requireAssumptions = Annotation("require", AnnotationStr("Assumptions"))
    val requireTypeOk = Annotation("require", AnnotationIdent("TypeOK"))
    val ensureAssertion = Annotation("ensure", AnnotationIdent("Assertion"))
    val testActionAnnotation = Annotation("testAction")
    store += testAction.ID -> List(requireCInit, requireAssumptions, requireTypeOk, ensureAssertion,
        testActionAnnotation)
    val testStatelessAnnotation = Annotation("testStateless")
    store += testStateless.ID -> List(requireCInit, testStatelessAnnotation)
    val requireLive = Annotation("require", AnnotationIdent("Live"))
    val requireSafe = Annotation("ensure", AnnotationIdent("Safe"))
    val testExecAnnotation = Annotation("testExecution", AnnotationInt(5))
    store += testExec.ID -> List(requireCInit, requireAssumptions, requireTypeOk, ensureAssertion, requireLive,
        requireSafe, testExecAnnotation)
  }
}
