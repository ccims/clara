package de.unistuttgart.iste.sqa.clara.export.graphviz

import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure

data class GraphVizExportFailure(val message: String) : ExportFailure("GraphViz", message)
