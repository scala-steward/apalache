package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.store.AnnotationStore
import at.forsyte.apalache.io.annotations.{Annotation, AnnotationInt}
import at.forsyte.apalache.tla.lir.TypedPredefs._
import at.forsyte.apalache.tla.lir.convenience.tla
import at.forsyte.apalache.tla.lir.transformations.TransformationTracker
import at.forsyte.apalache.tla.lir.{BoolT1, OperT1, TlaDecl, TlaEx, TlaModule, TlaOperDecl}
import at.forsyte.apalache.tla.script.ScriptExtractor.TestTriple
import com.typesafe.scalalogging.LazyLogging

/**
 * ScriptExtractor reads an annotated module and produces a series of tests in the normalized form,
 * which can be easily checked with other commands.
 *
 * @param store   annotation store
 * @param tracker transformation tracker
 */
class ScriptExtractor(store: AnnotationStore, tracker: TransformationTracker) extends LazyLogging {
  def apply(module: TlaModule): ScriptDashboard = {
    val triples = module.operDeclarations.flatMap(findTestCandidates(module))
    val testNames = Set(triples.map(_.decl.name): _*)
    val nonTestDefs = (for {
      d <- module.operDeclarations if !testNames.contains(d.name)
    } yield d.name -> d).toMap

    val dashboard = new ScriptDashboard
    for (triple <- triples) {
      isolateTestInModule(module, nonTestDefs)(triple) match {
        case Some((testModule, command)) =>
          dashboard.push(testModule, command)

        case None =>
          dashboard.addResultWithoutCommand(ScriptIgnored())
      }
    }

    dashboard
  }

  private def isolateTestInModule(inputModule: TlaModule, nonTestDefs: Map[String, TlaOperDecl])(
      testTriple: TestTriple): Option[(TlaModule, ScriptCommand)] = {
    // it is crucial to keep the original order of definitions, as SANY is sensitive to it
    val nonTestDefsSorted = inputModule.operDeclarations.filter(d => nonTestDefs.contains(d.name))

    testTriple.testAnnotation match {
      case Annotation("testStateless") =>
        val command = StatelessScriptCommand(testTriple.decl.name)
        // importantly, do not include variables, as they are not needed for stateless tests
        val moduleDecls: Seq[TlaDecl] =
          inputModule.constDeclarations ++
            inputModule.assumeDeclarations ++
            nonTestDefsSorted
        for {
          cinit <- groupKinds(nonTestDefs, command.testName, command.constInit,
              testTriple.kinds.collect { case r @ RequireConst(_) => r })
        } yield (TlaModule(command.testName, moduleDecls ++ Seq(cinit, testTriple.decl)), command)

      case Annotation("testAction") =>
        val command = ActionScriptCommand(testTriple.decl.name)
        val moduleDecls: Seq[TlaDecl] =
          inputModule.constDeclarations ++
            inputModule.assumeDeclarations ++
            inputModule.varDeclarations ++
            nonTestDefsSorted
        for {
          cinit <- groupKinds(nonTestDefs, command.testName, command.constInit,
              testTriple.kinds.collect { case r @ RequireConst(_) => r })
          pre <- groupKinds(nonTestDefs, command.testName, command.pre,
              testTriple.kinds.collect { case r @ RequireState(_) => r })
          post <- groupKinds(nonTestDefs, command.testName, command.post,
              testTriple.kinds.collect { case e @ EnsureAction(_) => e })
        } yield (TlaModule(command.testName, moduleDecls ++ Seq(cinit, pre, testTriple.decl, post)), command)

      case Annotation("testExecution", AnnotationInt(length)) =>
        val command = ExecScriptCommand(testTriple.decl.name, length)
        val moduleDecls: Seq[TlaDecl] =
          inputModule.constDeclarations ++
            inputModule.assumeDeclarations ++
            inputModule.varDeclarations ++
            nonTestDefsSorted
        for {
          cinit <- groupKinds(nonTestDefs, command.testName, command.constInit,
              testTriple.kinds.collect { case r @ RequireConst(_) => r })
          init <- groupKinds(nonTestDefs, command.testName, command.init,
              testTriple.kinds.collect { case r @ RequireState(_) => r })
          post <- groupKinds(nonTestDefs, command.testName, command.post,
              testTriple.kinds.collect { case e @ EnsureAction(_) => e })
          temporalPre <- groupKinds(nonTestDefs, command.testName, command.temporalPre,
              testTriple.kinds.collect { case e @ RequireTemporal(_) => e })
          temporalPost <- groupKinds(nonTestDefs, command.testName, command.temporalPost,
              testTriple.kinds.collect { case e @ EnsureTemporal(_) => e })
        } yield (TlaModule(command.testName,
                moduleDecls ++ Seq(cinit, init, testTriple.decl, post, temporalPre, temporalPost)), command)

      case name @ _ =>
        logger.error(s"Unsupported annotation type $name: " + testTriple.testAnnotation)
        None
    }
  }

  private def groupKinds(nonTestDefs: Map[String, TlaOperDecl], scriptName: String, groupName: String,
      kinds: Seq[RequireEnsureKind]): Option[TlaOperDecl] = {
    val bodies =
      kinds.flatMap { k =>
        nonTestDefs.get(k.name) match {
          case None =>
            logger.info(s"Test $scriptName needs operator ${k.name}, which is not found")
            None

          case Some(TlaOperDecl(_, params, body)) =>
            if (params.nonEmpty) {
              logger.info(
                  s"Test $scriptName needs operator ${k.name} to be parameterless, found ${params.length} parameters")
              None
            } else {
              Some(body)
            }
        }
      }

    if (bodies.length < kinds.length) {
      None
    } else {
      def mkOper(expr: TlaEx): TlaOperDecl = {
        tla.declOp(groupName, expr).typedOperDecl(OperT1(Seq(), BoolT1()))
      }

      bodies.toList match {
        case Nil =>
          // just produce TRUE
          Some(mkOper(tla.bool(true).typed(BoolT1())))

        case only :: Nil =>
          Some(mkOper(only))

        case many =>
          val groupBody = tla.and(many: _*).typed(BoolT1())
          Some(mkOper(groupBody))
      }
    }
  }

  private def findTestCandidates(module: TlaModule)(oneDef: TlaOperDecl): Option[TestTriple] = {
    store.get(oneDef.ID) match {
      case None | Some(List()) =>
        None

      case Some(annotations) =>
        val requireEnsureAnnotations = annotations.filter(a => a.name == "require" || a.name == "ensure")
        val parser = new RequireEnsureKindParser(module)
        val parseResults = requireEnsureAnnotations.map(parser.parse)
        // in scala 2.13, we could just use parseResults.partitionMap
        if (parseResults.exists(_.isLeft)) {
          for (error <- parseResults collect { case Left(e) => e }) {
            logger.error(s"Error in annotations of ${oneDef.name}: ${error.message}")
          }
          None
        } else {
          val kinds = parseResults.map(_.right.get)
          findTestAnnotation(oneDef.name, annotations) map { a => TestTriple(oneDef, a, kinds) }
        }
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

  private case class TestTriple(decl: TlaOperDecl, testAnnotation: Annotation, kinds: List[RequireEnsureKind]) {}
}
