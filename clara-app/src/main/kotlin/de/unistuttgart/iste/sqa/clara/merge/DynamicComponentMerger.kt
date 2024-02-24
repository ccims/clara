package de.unistuttgart.iste.sqa.clara.merge

import arrow.core.Either
import de.unistuttgart.iste.sqa.clara.api.merge.ComponentMerger
import de.unistuttgart.iste.sqa.clara.api.merge.MergeFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component

class DynamicComponentMerger : ComponentMerger {

    // First we need to filter the returned component types (Otel, Dns, etc)
    // Next we define a strict merging hierarchy: 1. DNS 2. Otel, 3. SBOM, 4. ?)
    // We take the one with the highest hierarchy as baseline
    // then we iterate over the first two service group's components and try to merge.
    // thereby, it is important that we somehow keep the reference of the original objects in the merged ones to merge the communications later.
    // then the next component group is merged into the previous result
    // in then end we have one list of components containing all information found

    // next we need to merge communications.
    // communications take generic source, target types and thus should work with the result.
    // we just need to make sure, that the merged component can still be matched, as the communications hold a reference to their object
    override fun merge(components: List<Component>, communications: List<Communication>): Pair<List<Either<MergeFailure, Component>>, List<Either<MergeFailure, Communication>>> {
        TODO("Not yet implemented")
    }
}