package at.forsyte.apalache.tla.typecomp.signatures

import at.forsyte.apalache.tla.lir._
import at.forsyte.apalache.tla.lir.oper.TlaOper
import at.forsyte.apalache.tla.typecomp.{BuilderUtil, SignatureMap}

/**
 * Produces a SignatureMap for all base operators
 *
 * @author
 *   Jure Kukovec
 */
object BaseOperSignatures {
  import TlaOper._
  import BuilderUtil._

  def getMap: SignatureMap = {

    // (t, t) => Bool
    val cmpSigs: SignatureMap = Seq(
        TlaOper.eq,
        TlaOper.ne,
    ).map { signatureMapEntry(_, { case Seq(t, tt) if t == tt => BoolT1 }) }.toMap

    //  ( (t1, ..., tn) => t, t1, ..., tn ) => t
    val applySig = signatureMapEntry(TlaOper.apply, { case OperT1(ts, t) +: tts if ts == tts => t })

    // (t, Set(t), Bool) => t
    val chooseBoundedSig = signatureMapEntry(chooseBounded, { case Seq(t, SetT1(tt), BoolT1) if t == tt => t })

    // (t, Bool) => t
    val chooseUnboundedSig = signatureMapEntry(chooseUnbounded, { case Seq(t, BoolT1) => t })

    // (t, t1, ..., tn) => t
    val labelSig = signatureMapEntry(label, { case t +: ts if ts.nonEmpty => t })

    cmpSigs + applySig + chooseBoundedSig + chooseUnboundedSig + labelSig
  }
}
