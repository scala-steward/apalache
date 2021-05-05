package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.{Annotation, AnnotationIdent}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class TestRequireEnsureKindParser extends FunSuite with BeforeAndAfter with ModuleFixture {
  private val parser: RequireEnsureKindParser = new RequireEnsureKindParser(fixtureModule)

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
