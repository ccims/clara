package de.unistuttgart.iste.sqa.clara.export.gropius

import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure

data class GropiusExportFailure(val message: String) : ExportFailure("Gropius", message)
