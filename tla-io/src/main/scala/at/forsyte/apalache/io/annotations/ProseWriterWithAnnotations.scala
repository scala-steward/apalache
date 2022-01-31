package at.forsyte.apalache.io.annotations

import at.forsyte.apalache.io.annotations.store.AnnotationStore
import at.forsyte.apalache.io.lir.{ProseWriter, TextLayout, TlaDeclAnnotator, TlaWriter}
import at.forsyte.apalache.tla.imp.AnnotationExtractor
import at.forsyte.apalache.tla.lir._

import java.io.PrintWriter

/**
 * A decorator of PrettyWriter that prints code annotations.
 *
 * @author Igor Konnov
 */
class ProseWriterWithAnnotations(annotationStore: AnnotationStore, writer: PrintWriter,
    layout: TextLayout = TextLayout())
    extends TlaWriter {

  private object annotator extends TlaDeclAnnotator {
    override def apply(layout: TextLayout)(decl: TlaDecl): Option[List[String]] = {
      val typeAnnotation: Option[List[String]] =
        decl.typeTag match {
          case Typed(tt: TlaType1) => Some(List(Annotation("type", AnnotationStr(tt.toString)).toPrettyString))
          case _                   => None
        }

      annotationStore.get(decl.ID) match {
        case None | Some(List()) =>
          typeAnnotation

        case Some(annotations) =>
          val annotationsAsStr = annotations map {
            case Annotation(AnnotationExtractor.FREE_TEXT, AnnotationStr(text)) =>
              // return the text as as
              text.trim()

            case other =>
              // wrap with markdown quotes to display verbatim
              "```" + other.toPrettyString + "```"
          }
          Some(typeAnnotation.getOrElse(List()) ++ annotationsAsStr)
      }
    }
  }

  private val englishWriter: ProseWriter = new ProseWriter(writer, layout, annotator)

  /**
   * Write a module, including all declarations
   *
   * @param mod a module
   */
  override def write(mod: TlaModule, extendedModuleNames: List[String]): Unit = {
    englishWriter.write(mod, extendedModuleNames)
  }

  /**
   * Write a declaration, including all expressions
   *
   * @param decl a declaration
   */
  override def write(decl: TlaDecl): Unit = {
    englishWriter.write(decl)
  }

  /**
   * Write a TLA+ expression.
   *
   * @param expr an expression
   */
  override def write(expr: TlaEx): Unit = {
    englishWriter.write(expr)
  }
}
