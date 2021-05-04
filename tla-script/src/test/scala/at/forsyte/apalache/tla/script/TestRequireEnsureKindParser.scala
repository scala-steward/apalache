package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.{Annotation, AnnotationIdent}
import at.forsyte.apalache.tla.lir.UntypedPredefs._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.{TlaConstDecl, TlaModule, TlaOperDecl, TlaVarDecl}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class TestRequireEnsureKindParser extends FunSuite with BeforeAndAfter {
  private val constN = TlaConstDecl("N")
  private val varX = TlaVarDecl("x")
  private val constInit = TlaOperDecl("ConstInit", List(), tla.eql(tla.name("N"), tla.int(10)))
  private val assumptions = TlaOperDecl("Assumptions", List(), tla.lt(tla.name("N"), tla.int(5)))
  private val typeOk = TlaOperDecl("TypeOK", List(), tla.in(tla.name("x"), tla.dotdot(tla.int(1), tla.name("N"))))
  private val assertion = TlaOperDecl("Assertion", List(), tla.lt(tla.prime(tla.name("x")), tla.name("x")))
  private val safe = TlaOperDecl("Safe", List(), tla.box(tla.eql(tla.name("x"), tla.int(2))))
  private val live = TlaOperDecl("Live", List(), tla.diamond(tla.eql(tla.name("x"), tla.int(2))))

  private val module: TlaModule =
    TlaModule("root", Seq(constN, varX, constInit, assumptions, typeOk, assertion, safe, live))
  private val parser: RequireEnsureKindParser = new RequireEnsureKindParser(module)

  test("parse ConstInit") {

    val output = parser.parse(Annotation("require", AnnotationIdent("ConstInit")))
    val expected = Right(RequireConst("ConstInit"))
    assert(expected == output)
  }

  test("parse Assumptions") {
    val output = parser.parse(Annotation("require", AnnotationIdent("Assumptions")))
    val expected = Right(RequireConst("Assumptions"))
    assert(expected == output)
  }

  test("parse TypeOK") {
    val output = parser.parse(Annotation("require", AnnotationIdent("TypeOK")))
    val expected = Right(RequireState("TypeOK"))
    assert(expected == output)
  }

  test("parse Assertion") {
    val output = parser.parse(Annotation("ensure", AnnotationIdent("Assertion")))
    val expected = Right(EnsureAction("Assertion"))
    assert(expected == output)
  }

  test("parse Safe") {
    val output = parser.parse(Annotation("require", AnnotationIdent("Safe")))
    val expected = Right(RequireTemporal("Safe"))
    assert(expected == output)
  }

  test("parse Live") {
    val output = parser.parse(Annotation("ensure", AnnotationIdent("Live")))
    val expected = Right(EnsureTemporal("Live"))
    assert(expected == output)
  }
}
