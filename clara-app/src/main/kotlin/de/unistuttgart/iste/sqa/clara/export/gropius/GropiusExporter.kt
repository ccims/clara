package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.*
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.export.onFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.ComponentType
import de.unistuttgart.iste.sqa.clara.api.model.toComponent
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GraphQLClient
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GropiusGraphQLClient
import de.unistuttgart.iste.sqa.gropius.*
import de.unistuttgart.iste.sqa.gropius.getallcomponenttemplates.ComponentTemplate
import de.unistuttgart.iste.sqa.gropius.getallrelationtemplates.RelationTemplate
import de.unistuttgart.iste.sqa.gropius.getcomponentversionbyid.ComponentVersion
import de.unistuttgart.iste.sqa.gropius.inputs.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.net.URL

class GropiusExporter(private val config: Config) : Exporter {

    data class Config(
        val projectId: String,
        val graphQLBackendUrl: URL,
        val graphQLBackendAuthentication: Authentication,
        val gropiusComponentHandling: ComponentHandling,
    ) {

        enum class ComponentHandling {
            Modify,
            Delete,
        }

        data class Authentication(
            val authenticationUrl: URL,
            val userName: String,
            val password: String,
            val clientId: String,
        )
    }

    private val log = KotlinLogging.logger {}

    private val graphQLClient: GraphQLClient = GropiusGraphQLClient(
        backendUrl = config.graphQLBackendUrl,
        authenticationUrl = config.graphQLBackendAuthentication.authenticationUrl,
        userName = config.graphQLBackendAuthentication.userName,
        password = config.graphQLBackendAuthentication.password,
        clientId = config.graphQLBackendAuthentication.clientId,
    )
    companion object {
        private const val VERSION_DESCRIPTION_FALLBACK = "-"
    }
    override fun export(components: Set<Component>, communications: Set<Communication>): Either<GropiusExportFailure, Unit> = runBlocking {
        log.info { "Export to Gropius ..." }

        checkProject().onFailure { return@runBlocking it }

        val (componentTemplates, relationTemplates) = getDatasetTemplates().getOrElse { return@runBlocking Either.Left(it) }

        val dataSetComponents = addDatasetComponents(components, componentTemplates, relationTemplates).getOrElse { return@runBlocking Either.Left(it) }

        addRelations(communications, dataSetComponents, relationTemplates).onFailure { return@runBlocking it }

        log.info { "Done exporting to Gropius" }

        Either.Right(Unit)
    }

    private suspend fun checkProject(): Either<GropiusExportFailure, Unit> {
        log.debug { "Checking that the Gropius project exists" }

        return when (val projectResult = graphQLClient.execute(GetProjectById(GetProjectById.Variables(id = config.projectId)))) {
            is Either.Left -> Either.Left(GropiusExportFailure("Cannot validate the existence of the project with the ID ${config.projectId} (${projectResult.value})"))
            is Either.Right -> if (projectResult.value.projects.nodes.none { it.id == config.projectId }) {
                Either.Left(GropiusExportFailure("The project with the ID ${config.projectId} does not exist!"))
            } else {
                val project = projectResult.value.projects.nodes.first()
                log.info { "Using Gropius project ${project.name} (${project.id})" }
                Either.Right(Unit)
            }
        }
    }

    private suspend fun getDatasetTemplates(): Either<GropiusExportFailure, Pair<List<ComponentTemplate>, List<RelationTemplate>>> {
        val componentTemplatesResult = graphQLClient.execute(GetAllComponentTemplates())
        val relationTemplatesResult = graphQLClient.execute(GetAllRelationTemplates())

        val componentTemplates = componentTemplatesResult
            .map { result -> result.componentTemplates.nodes }
            .getOrElse { error -> return Either.Left(GropiusExportFailure("Failed to query all component templates ($error)")) }

        val relationTemplates = relationTemplatesResult
            .map { result -> result.relationTemplates.nodes }
            .getOrElse { error -> return Either.Left(GropiusExportFailure("Failed to query all relation templates ($error)")) }

        // TODO: check if required templates exist (see clara config)

        // val neededComponentTemplates = emptyList<String>()
        // val neededRelationTemplates = emptyList<String>()

        // TODO: generate required templates from
        /*
        val resultAddComponentTemplates = run {  // does batch processing work here?
            for (tmplComponent in neededComponentTemplates) {
                graphQLClient.execute(CreateComponentTemplate(tmplComponent))
            }

            for (tmplRelation in neededRelationTemplates) {
                graphQLClient.execute(CreateRelationTemplate(tmplRelation))
            }
        }
         */
        return Either.Right(componentTemplates to relationTemplates)
    }

    private suspend fun addDatasetComponents(
        components: Set<Component>,
        componentTemplates: List<ComponentTemplate>,
        relationTemplates: List<RelationTemplate>,
    ): Either<GropiusExportFailure, Set<GropiusComponent>> {

        val gropiusComponents = getAllComponents().getOrElse { return Either.Left(it) }
        val createdGropiusComponents = mutableSetOf<GropiusComponent>()

        for (component in components) {

            val (componentId, componentVersionId) = createOrUpdateComponent(component, gropiusComponents, componentTemplates).getOrElse { return Either.Left(it) }
            val gropiusComponentId = GropiusComponent.ComponentId(componentId)
            val gropiusVersionId = GropiusComponent.ComponentVersionId(componentVersionId)
            createdGropiusComponents.add(GropiusComponent(component, gropiusComponentId, gropiusVersionId))

            // Add componentVersion to project
            addComponentVersionToProject(componentVersionId, config.projectId)

            // TODO FIXME
            // addLibrariesToComponent(componentVersionId, component, componentTemplates, relationTemplates)
        }
        return Either.Right(createdGropiusComponents)
    }

    private suspend fun createOrUpdateComponent(
        component: Component,
        gropiusComponents: List<de.unistuttgart.iste.sqa.gropius.getallcomponents.Component>,
        componentTemplates: List<ComponentTemplate>,
    ): Either<GropiusExportFailure, Pair<ID, ID>> {
        val searchResult = gropiusComponents.find { it.name == component.name.value }
        val componentId = when {
            searchResult == null -> createComponent(component, componentTemplates)
            config.gropiusComponentHandling == Config.ComponentHandling.Modify -> {
                if (searchResult.name != component.name.value || searchResult.description != component.getDescription() || searchResult.template.id != component.getTemplate(componentTemplates)) {
                    updateComponent(searchResult.id, component, componentTemplates)
                } else {
                    Either.Right(searchResult.id)
                }
            }

            else -> {
                deleteComponent(searchResult.id)
                createComponent(component, componentTemplates)
            }
        }.getOrElse { return Either.Left(it) }

        val componentVersionSearchResult = getComponentVersionOrNull(componentId).getOrElse { return Either.Left(it) }
        val componentVersionId = when {
            component.version == null && componentVersionSearchResult?.version == VERSION_DESCRIPTION_FALLBACK -> componentVersionSearchResult.id
            component.version?.value == componentVersionSearchResult?.version && componentVersionSearchResult != null -> componentVersionSearchResult.id
            else -> createComponentVersion(componentId, component).getOrElse { return Either.Left(it) }
        }

        return Either.Right(componentId to componentVersionId)
    }

    private suspend fun getAllComponents(): Either<GropiusExportFailure, List<de.unistuttgart.iste.sqa.gropius.getallcomponents.Component>> {
        val result = graphQLClient.execute(GetAllComponents()).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to get all components: $error"))
        }
        return Either.Right(result.components.nodes)
    }

    private suspend fun createComponent(component: Component, componentTemplates: List<ComponentTemplate>): Either<GropiusExportFailure, ID> {
        val componentResult = graphQLClient.execute(
            CreateComponent(
                CreateComponent.Variables(
                    description = component.getDescription(),
                    name = component.name.value,
                    template = component.getTemplate(componentTemplates),
                    repositoryURL = "https://example.org"
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create component with name '${component.name}': $error"))
        }

        return Either.Right(componentResult.createComponent.component.id)
    }

    private suspend fun deleteComponent(componentId: ID): Either<GropiusExportFailure, ID> {
        val result = graphQLClient.execute(
            DeleteComponent(
                DeleteComponent.Variables(
                    id = componentId
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to delete the component '${componentId}': $error"))
        }

        return Either.Right(result.deleteComponent.id)
    }

    private suspend fun updateComponent(componentId: ID, component: Component, componentTemplates: List<ComponentTemplate>): Either<GropiusExportFailure, ID> {
        val componentResult = graphQLClient.execute(
            UpdateComponent(
                UpdateComponent.Variables(
                    id = componentId,
                    description = component.getDescription(),
                    template = component.getTemplate(componentTemplates)
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to update component with name '${component.name}' (ID=$componentId): $error"))
        }

        return Either.Right(componentResult.updateComponent.component.id)
    }

    private suspend fun createComponents(
        newComponents: List<Component>,
        componentTemplates: List<ComponentTemplate>,
    ): Either<GropiusExportFailure, List<ID>> {
        val componentResult = graphQLClient.execute(
            BulkCreateComponent(
                BulkCreateComponent.Variables(
                    BulkCreateComponentInput(
                        newComponents.map { it.toCreateComponentInput(componentTemplates) }
                    )
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create bulk components: $error"))
        }

        return Either.Right(componentResult.bulkCreateComponent.components.map { it.id })
    }

    private fun Component.toCreateComponentInput(componentTemplates: List<ComponentTemplate>): CreateComponentInput = CreateComponentInput(
        description = getDescription(),
        name = name.value,
        template = getTemplate(componentTemplates),
        repositoryURL = "https://example.org",
        templatedFields = emptyList(),
        versions = listOf(ComponentVersionInput(
            description = version?.let { "v$it" } ?: VERSION_DESCRIPTION_FALLBACK,
            name = name.value,
            version = version?.value ?: VERSION_DESCRIPTION_FALLBACK,
            templatedFields = emptyList(),
        ))
    )

    private suspend fun addLibrariesToComponent(
        componentVersionId: ID,
        component: Component,
        componentTemplates: List<ComponentTemplate>,
        relationTemplates: List<RelationTemplate>,
    ): Either<GropiusExportFailure, Unit> {
        if (component is Component.InternalComponent) {
            val gropiusComponents = getAllComponents().getOrElse { return Either.Left(it) }

            val libraryComponents = component.libraries?.let { libraries -> libraries.map { it.toComponent() } }?.filter { it.name != component.name } ?: emptyList()
            val filteredComponents = libraryComponents.filter { newComponent -> gropiusComponents.none { existingComponent -> existingComponent.name == newComponent.name.value } }
            createComponents(newComponents = filteredComponents, componentTemplates = componentTemplates).getOrElse { return Either.Left(it) }

            // It could be that a component is not newly created but needs a relation, so we fetch and filter a second time
            val updatedGropiusComponents = getAllComponents().getOrElse { return Either.Left(it) }

            // From all components find the one with the matching name and from its versions the one with the matching version and return the ID of version.
            val libraryComponentVersionIds = updatedGropiusComponents.filter { gropiusComponent -> libraryComponents.any { it.name.value == gropiusComponent.name } }
                .mapNotNull { it.versions.nodes.find { libraryComponents.any { libraryComponents -> libraryComponents.version?.value == it.version } }?.id }

            createLibraryRelations(start = componentVersionId, ends = libraryComponentVersionIds, relationTemplates.find { it.name == "Includes" }?.id.toString())
        }
        return Either.Right(Unit)
    }

    private suspend fun createComponentVersion(componentId: ID, component: Component): Either<GropiusExportFailure, ID> {
        val result = graphQLClient.execute(
            CreateComponentVersion(
                CreateComponentVersion.Variables(
                    component = componentId,
                    description = component.version?.let { "v$it" } ?: VERSION_DESCRIPTION_FALLBACK,
                    name = component.name.value,
                    version = component.version?.value ?: VERSION_DESCRIPTION_FALLBACK,
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create the component version for component with name '${component.name}' (ID=$componentId): $error"))
        }

        return Either.Right(result.createComponentVersion.componentVersion.id)
    }

    private suspend fun getComponentVersionOrNull(componentId: ID): Either<GropiusExportFailure, ComponentVersion?> {
        val result = graphQLClient.execute(
            GetComponentVersionById(
                GetComponentVersionById.Variables(
                    id = componentId
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to get component version '$componentId': $error"))
        }
        return Either.Right(result.components.nodes.first().versions.nodes.firstOrNull())
    }

    private suspend fun addComponentVersionToProject(componentVersionId: ID, projectId: String): Either<GropiusExportFailure, Unit> {
        graphQLClient.execute(
            AddComponentVersionToProject(
                AddComponentVersionToProject.Variables(
                    project = config.projectId,
                    componentVersion = componentVersionId
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to add the component version '$componentVersionId' to the project '${projectId}': $error"))
        }
        return Either.Right(Unit)
    }

    private suspend fun addRelations(
        communications: Set<Communication>,
        gropiusComponents: Set<GropiusComponent>,
        relationTemplates: List<RelationTemplate>,
    ): Either<GropiusExportFailure, Unit> {
        for (communication in communications) {
            val start = gropiusComponents.find { it.component.name == communication.source.componentName }?.componentVersionId
            val end = gropiusComponents.find { it.component.name == communication.target.componentName }?.componentVersionId
            if (start == null || end == null) {
                log.warn { "No relation can be added. Start: ${start?.value} End: ${end?.value}" }
            } else {
                createRelation(start.value, end.value, template = relationTemplates.find { it.name == "General relation" }?.id.toString())
            }
        }

        return Either.Right(Unit)
    }

    private suspend fun createRelation(start: ID, end: ID, template: String): Either<GropiusExportFailure, Unit> {
        graphQLClient.execute(
            CreateRelation(
                CreateRelation.Variables(
                    start = start,
                    end = end,
                    relTemplateId = template
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create the relation from '$start' to '$end': $error"))
        }
        return Either.Right(Unit)
    }

    private suspend fun createLibraryRelations(start: ID, ends: List<ID>, template: String): Either<GropiusExportFailure, Unit> {
        val createRelationInputs = ends.map { CreateRelationInput(start = start, end = it, template = template, templatedFields = emptyList()) }
        graphQLClient.execute(
            BulkCreateRelation(
                BulkCreateRelation.Variables(
                    BulkCreateRelationInput(
                        createRelationInputs
                    )
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create bulk relations: $error"))
        }
        return Either.Right(Unit)
    }
}

private fun Component.getDescription(): String {
    return when {
        this is Component.InternalComponent -> "IP-address: ${ipAddress?.value ?: "unknown"}"
        this is Component.ExternalComponent && type == ComponentType.Library -> "Library: ${name.value}"
        this is Component.ExternalComponent -> "Domain: ${domain.value}"
        else -> ""
    }
}

private fun Component.getTemplate(componentTemplates: List<ComponentTemplate>): String = when {
    this is Component.InternalComponent && type == null -> componentTemplates.find { it.name == "Base component template" }?.id.toString()
    this is Component.InternalComponent && type == ComponentType.Broker -> componentTemplates.find { it.name == "Messaging" }?.id.toString()
    this is Component.InternalComponent && type == ComponentType.Database -> componentTemplates.find { it.name == "Database" }?.id.toString()
    this is Component.InternalComponent && type == ComponentType.Microservice -> componentTemplates.find { it.name == "Microservice" }?.id.toString()
    this is Component.ExternalComponent && type == ComponentType.Library -> componentTemplates.find { it.name == "Library" }?.id.toString()
    this is Component.ExternalComponent -> componentTemplates.find { it.name == "Misc" }?.id.toString()
    else -> ""
}
