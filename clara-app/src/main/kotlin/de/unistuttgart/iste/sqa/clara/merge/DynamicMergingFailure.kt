package de.unistuttgart.iste.sqa.clara.merge

import de.unistuttgart.iste.sqa.clara.api.merge.MergeFailure

data class DynamicMergingFailure(val message: String) : MergeFailure("Dynamic merging", message)
