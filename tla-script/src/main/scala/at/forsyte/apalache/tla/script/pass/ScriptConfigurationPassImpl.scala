package at.forsyte.apalache.tla.script.pass

import at.forsyte.apalache.infra.passes.{Pass, PassOptions, TlaModuleMixin}
import at.forsyte.apalache.io.annotations.store.AnnotationStore
import at.forsyte.apalache.tla.imp.src.SourceStore
import at.forsyte.apalache.tla.lir.io.{TlaWriter, TlaWriterFactory}
import at.forsyte.apalache.tla.lir.transformations.TransformationTracker
import at.forsyte.apalache.tla.script.ScriptExtractor
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging

import java.io.File

class ScriptConfigurationPassImpl @Inject() (val options: PassOptions, val sourceStore: SourceStore,
    tracker: TransformationTracker, val annotationStore: AnnotationStore, tlaWriterFactory: TlaWriterFactory,
    @Named("AfterScriptConfiguration") val nextPass: Pass with TlaModuleMixin)
    extends ScriptConfigurationPass with LazyLogging {

  /**
   * The name of the pass
   *
   * @return the name associated with the pass
   */
  override def name: String = "ScriptConfiguration"

  /**
   * Run the pass.
   *
   * @return true, if the pass was successful
   */
  override def execute(): Boolean = {
    logger.info(" > Configuring tests and scripts...")
    if (tlaModule.isEmpty) {
      false
    } else {
      val dashboard = new ScriptExtractor(annotationStore, tracker)(tlaModule.get)
      // In the future, we will run the tests in the dashboard. For now, we just write them to files.
      while (dashboard.queueLen > 0) {
        val (testModule, testScript) = dashboard.pop().get
        tlaWriterFactory.writeModuleAllFormats(testModule, TlaWriter.STANDARD_MODULES, new File("."))
        logger.info(s"Wrote test ${testModule.name}")
      }
      logger.info(s"Ignored tests: ${dashboard.stats.nIgnored}")
      true
    }
  }

  /**
   * Get the next pass in the chain. What is the next pass is up
   * to the module configuration and the pass outcome.
   *
   * @return the next pass, if exists, or None otherwise
   */
  override def next(): Option[Pass] = None
}
