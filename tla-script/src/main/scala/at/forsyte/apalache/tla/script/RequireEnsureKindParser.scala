package at.forsyte.apalache.tla.script

import at.forsyte.apalache.io.annotations.{Annotation, AnnotationIdent, AnnotationStr}
import at.forsyte.apalache.tla.lir.{
  TlaLevel, TlaLevelAction, TlaLevelConst, TlaLevelFinder, TlaLevelState, TlaLevelTemporal, TlaModule
}

/**
 * Parser of require/ensure annotations that classifies the annotations more precisely than the user.
 *
 * @param module TLA module
 */
class RequireEnsureKindParser(module: TlaModule) {
  private val consts = Set(module.constDeclarations.map(_.name): _*)
  private val vars = Set(module.varDeclarations.map(_.name): _*)
  private val defs = Map(module.operDeclarations map { d => d.name -> d }: _*)
  private var levelCacheMap: Map[String, TlaLevel] = Map()

  def parse(annotation: Annotation): Either[ScriptError, RequireEnsureKind] = {
    findOperatorName(annotation) flatMap { name =>
      if (!defs.contains(name)) {
        Left(ScriptError(name, s"Operator $name not found"))
      } else {
        cacheLevel(name)
        (annotation.name, levelCacheMap(name)) match {
          case ("require", TlaLevelConst) =>
            Right(RequireConst(name))
          case ("ensure", TlaLevelConst) =>
            Left(ScriptError(name, s"Operator $name has constant level, unexpected in @ensure"))
          case ("require", TlaLevelState) =>
            Right(RequireState(name))
          case ("ensure", TlaLevelState) =>
            Left(ScriptError(name, s"Operator $name has action level, unexpected in @ensure"))
          case ("require", TlaLevelAction) =>
            Left(ScriptError(name, s"Operator $name has action level, unexpected in @require"))
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

  private def findOperatorName(annotation: Annotation): Either[ScriptError, String] = {
    annotation.args match {
      case Seq(AnnotationIdent(name)) =>
        Right(name)

      case Seq(AnnotationStr(text)) =>
        Right(text.trim)

      case _ =>
        val msg = "Unexpected arguments in %s: %s".format(annotation.name, annotation.toPrettyString)
        Left(ScriptError(annotation.name, msg))
    }
  }

  private def cacheLevel(name: String): Unit = {
    def levelOfName(name: String): TlaLevel = {
      if (consts.contains(name)) {
        TlaLevelConst
      } else if (vars.contains(name)) {
        TlaLevelState
      } else {
        if (defs.contains(name)) {
          // as the module comes from the parser, we assume that defs contains a definition for the name `name`
          cacheLevel(name)
          levelCacheMap(name)
        } else {
          TlaLevelConst
        }
      }
    }

    val level = new TlaLevelFinder(levelOfName)(defs(name).body)
    levelCacheMap += name -> level
  }

}
