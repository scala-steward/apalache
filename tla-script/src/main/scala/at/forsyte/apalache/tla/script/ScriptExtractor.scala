package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.Annotation
import at.forsyte.apalache.io.annotations.store.AnnotationStore
import at.forsyte.apalache.tla.lir.{TlaModule, TlaOperDecl}
import at.forsyte.apalache.tla.lir.transformations.TransformationTracker
import at.forsyte.apalache.tla.script.ScriptExtractor.TestTriple
import com.typesafe.scalalogging.LazyLogging

class ScriptExtractor(store: AnnotationStore, tracker: TransformationTracker) extends LazyLogging {
  def apply(module: TlaModule): List[TlaModule] = {
    val triples = module.operDeclarations.flatMap(findTestCandidates)
    val testNames = Set(triples.map(_.decl.name): _*)
    val nonTestDefs = (for {
      d <- module.operDeclarations if !testNames.contains(d.name)
    } yield d.name -> d).toMap
    triples.flatMap(isolateTestInModule(module, nonTestDefs)).toList
  }

  private def isolateTestInModule(inputModule: TlaModule, nonTestDefs: Map[String, TlaOperDecl])(
      testTriple: TestTriple): Option[TlaModule] = {
    testTriple.testAnnotation.name match {
      case "testStateless" =>
        Some(inputModule)

      case "testAction" =>
        Some(inputModule)

      case "testExecution" =>
        Some(inputModule)

      case name @ _ =>
        logger.error(s"Unsupported annotation type $name")
        None
    }
  }

  //  private def groupConstInit(inputModule: TlaModule, consts: Set[String]): Option[TlaOperDecl] = {}

  private def findTestCandidates(oneDef: TlaOperDecl): Option[TestTriple] = {
    store.get(oneDef.ID) match {
      case None | Some(List()) =>
        None

      case Some(annotations) =>
        findTestAnnotation(oneDef.name, annotations) map { a => TestTriple(oneDef, a, annotations) }
    }
  }

  private def findTestAnnotation(name: String, annotations: List[Annotation]): Option[Annotation] = {
    annotations.filter(a => {
      ScriptExtractor.TEST_ANNOTATION_NAMES.contains(a.name)
    }) match {
      case Nil =>
        None

      case one :: Nil =>
        Some(one)

      case many =>
        val separated = many.map("@" + _.name).mkString(", ")
        logger.error(s"Operator $name has multiple test annotations (skipped): $separated")
        None
    }
  }
}

object ScriptExtractor {
  val STATELESS = "testStateless"
  val ACTION = "testAction"
  val EXECUTION = "testExecution"
  val TEST_ANNOTATION_NAMES = Set(STATELESS, ACTION, EXECUTION)

  private case class TestTriple(decl: TlaOperDecl, testAnnotation: Annotation, otherAnnotations: List[Annotation]) {}
}
