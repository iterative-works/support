package mdr.pdb
package proof

object ProofEventHandler:

  extension (maybeProof: Option[Proof])
    // Returns None if the event could not be handled, otherwise the result is always some Proof
    def handleEvent(event: ProofEvent): Option[Proof] =
      type ProofHandler = PartialFunction[Event, Proof]
      type ProofModHandler = PartialFunction[Event, Proof => Proof]

      val ProofEvent(ev, ww) = event

      val handleCreateProof: ProofHandler = {
        case ProofCreated(id, person, parameterId, criterionId, documents) =>
          Proof(id, person, parameterId, criterionId, documents, Nil, Nil, ww)
      }

      val handleAuthorizeProof: ProofModHandler = {
        case ProofAuthorized(id, note) =>
          proof =>
            proof.copy(authorizations =
              proof.authorizations :+ Authorization(ww, note)
            )
      }

      val handleUpdateProof: ProofModHandler = {
        case ProofUpdated(id, documents) =>
          proof => proof.copy(documents = documents)
      }

      val handleRevokeProof: ProofModHandler = {
        case ProofRevoked(id, reason, since, documents) =>
          proof =>
            proof.copy(revocations =
              proof.revocations :+ Revocation(ww, since, reason, documents)
            )
      }

      def handle(h: ProofHandler): Option[Proof] =
        h.lift(ev)

      def handleMod(h: ProofModHandler): Option[Proof => Proof] =
        h.lift(ev)

      maybeProof match
        case None =>
          handle(handleCreateProof)
        case Some(p) =>
          handleMod {
            handleAuthorizeProof orElse handleUpdateProof orElse handleRevokeProof
          } map (_(p))
