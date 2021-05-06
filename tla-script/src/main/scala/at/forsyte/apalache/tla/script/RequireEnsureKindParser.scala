package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.{Annotation, AnnotationIdent, AnnotationStr}
import at.forsyte.apalache.tla.lir._

/**
 * Parser of require/ensure annotations that classifies the annotations more precisely than the user.
 *
 * @param module TLA module
 */
class RequireEnsureKindParser(module: TlaModule) {
  private val levelFinder = new TlaDeclLevelFinder(module)
  private val defs = Map(module.operDeclarations map { d => d.name -> d }: _*)

  def parse(annotation: Annotation): Either[ScriptFailure, RequireEnsureKind] = {
    findOperatorName(annotation) flatMap { name =>
      defs.get(name) match {
        case None =>
          Left(ScriptFailure(s"Operator $name not found"))

        case Some(decl) =>
          (annotation.name, levelFinder(decl)) match {
            case ("require", TlaLevelConst) =>
              Right(RequireConst(name))
            case ("ensure", TlaLevelConst) =>
              Left(ScriptFailure(s"Operator $name has constant level, unexpected in @ensure"))
            case ("require", TlaLevelState) =>
              Right(RequireState(name))
            case ("ensure", TlaLevelState) =>
              Left(ScriptFailure(s"Operator $name has action level, unexpected in @ensure"))
            case ("require", TlaLevelAction) =>
              Left(ScriptFailure(s"Operator $name has action level, unexpected in @require"))
            case ("ensure", TlaLevelAction) =>
              Right(EnsureAction(name))
            case ("require", TlaLevelTemporal) =>
              Right(RequireTemporal(name))
            case ("ensure", TlaLevelTemporal) =>
              Right(EnsureTemporal(name))
          }
      }
    }
  }

  private def findOperatorName(annotation: Annotation): Either[ScriptFailure, String] = {
    annotation.args match {
      case Seq(AnnotationIdent(name)) =>
        Right(name)

      case Seq(AnnotationStr(text)) =>
        Right(text.trim)

      case _ =>
        val msg = "Unexpected arguments in %s: %s".format(annotation.name, annotation.toPrettyString)
        Left(ScriptFailure(msg))
    }
  }
}
