package de.unistuttgart.iste.sqa.clara.merge

import de.unistuttgart.iste.sqa.clara.api.merge.MergeFailure

data class DefaultMergeFailure(val message: String) : MergeFailure("Merging", message)
