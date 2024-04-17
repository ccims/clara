package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.getOrElse
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.export.onFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.api.model.ComponentType
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GraphQLClient
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GropiusGraphQLClient
import de.unistuttgart.iste.sqa.gropius.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
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

    override fun export(components: Set<Component>, communications: Set<Communication>): Either<GropiusExportFailure, Unit> = runBlocking {
        log.info { "Export to Gropius ..." }

        checkProject().onFailure { return@runBlocking it }

        checkDatasetTemplates().onFailure { return@runBlocking it }

        // TODO do something with libraries
        val dataSetComponents = addDatasetComponents(components).getOrElse { return@runBlocking Either.Left(it) }

        addRelations(communications, dataSetComponents).onFailure { return@runBlocking it }

        log.info { "Done exporting to Gropius" }

        Either.Right(Unit)
    }

    private suspend fun checkProject(): Either<GropiusExportFailure, Unit> {
        log.debug { "Checking that the Gropius project exists" }

        val projectResult = graphQLClient.execute(GetProjectById(GetProjectById.Variables(id = config.projectId)))

        return when (projectResult) {
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

    private suspend fun checkDatasetTemplates(): Either<GropiusExportFailure, Unit> {
        // TODO: are the component scoped under the project ID?
        val componentTemplatesResult = graphQLClient.execute(GetAllComponentTemplates())
        val relationTemplatesResult = graphQLClient.execute(GetAllRelationTemplates())

        val componentTemplates = componentTemplatesResult
            .map { result -> result.componentTemplates.nodes }
            .getOrElse { error -> return Either.Left(GropiusExportFailure("Failed to query all component templates ($error)")) }

        val relationTemplates = relationTemplatesResult
            .map { result -> result.relationTemplates.nodes }
            .getOrElse { error -> return Either.Left(GropiusExportFailure("Failed to query all relation templates ($error)")) }

        // TODO: check if required templates exist (see clara config)

        val neededComponentTemplates = emptyList<String>()
        val neededRelationTemplates = emptyList<String>()

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

        // TODO: error handling

        return Either.Right(Unit)
    }

    private suspend fun addDatasetComponents(components: Set<Component>): Either<GropiusExportFailure, Set<GropiusComponent>> {
        val resultComponents = graphQLClient.execute(GetAllComponents()).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to get all components: $error"))
        }

        val createdGropiusComponents = mutableSetOf<GropiusComponent>()

        for (component in components) {
            val searchResult = resultComponents.components.nodes.find { it.name == component.name.value }
            val componentId = when {
                searchResult == null -> createComponent(component)
                config.gropiusComponentHandling == Config.ComponentHandling.Modify -> updateComponent(searchResult.id, component, None)
                else -> {
                    deleteComponent(searchResult.id)
                    createComponent(component)
                }
            }.getOrElse { return Either.Left(it) }

            // TODO: decide if and how detected components are congruent with db entries (match by name, ip, id, ...)

            val componentVersionIdOrNull = getComponentVersionOrNull(componentId).getOrElse { return Either.Left(it) }
            val componentVersionId = componentVersionIdOrNull ?: createComponentVersion(componentId, component).getOrElse { return Either.Left(it) }

            val gropiusComponentId = GropiusComponent.ComponentId(componentId)
            val gropiusVersionId = GropiusComponent.ComponentVersionId(componentVersionId)
            createdGropiusComponents.add(GropiusComponent(component, gropiusComponentId, gropiusVersionId))

            // Add componentVersion to project
            addComponentVersionToProject(componentVersionId, config.projectId)
        }
        return Either.Right(createdGropiusComponents)
    }

    private suspend fun createComponent(component: Component): Either<GropiusExportFailure, ID> {
        val (description, template) = when {
            // !!! also update "updateComponent" when changing this ↓ !!!
            component is Component.InternalComponent && component.type == ComponentType.Broker -> Pair("IP-address: ${component.ipAddress?.value ?: "unknown"}", "acb00484-82d3-427d-95dc-ddbf742f943f") // database template
            component is Component.InternalComponent && component.type == ComponentType.Database -> Pair("IP-address: ${component.ipAddress?.value ?: "unknown"}", "c5bda6c5-0e40-471e-95e6-800d546e8641") // database template
            component is Component.InternalComponent && component.type == ComponentType.Microservice -> Pair("IP-address: ${component.ipAddress?.value ?: "unknown"}", "35fa0bff-5d21-463e-8806-0151cfb718d4") // microservice template
            component is Component.InternalComponent && component.type == null -> Pair("IP-address: ${component.ipAddress?.value ?: "unknown"}", "796598e5-60d2-4759-adce-c439b5d4dc92") // general template
            component is Component.ExternalComponent -> Pair("Domain: ${component.domain.value}", "27c64acc-1bde-4bf8-ab03-d12f5c413dd2") // misc template
            else -> return Either.Left(GropiusExportFailure("this combination of component and component type should not be possible for '${component.name}'"))
        }

        val componentResult = graphQLClient.execute(
            CreateComponent(
                CreateComponent.Variables(
                    description = description,
                    name = component.name.value,
                    template = template,
                    repositoryURL = "https://example.org" // TODO: in the future we might want to add the repo link gathered from the SBOM here
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

    private suspend fun updateComponent(componentId: ID, component: Component, templateId: Option<String>): Either<GropiusExportFailure, ID> {
        val description = when (component) {
            // !!! also update "createComponent" when changing this ↓ !!!
            is Component.InternalComponent -> "IP-address: ${component.ipAddress?.value ?: "unknown"}"
            is Component.ExternalComponent -> "Domain: ${component.domain.value}"
        }

        val componentResult = graphQLClient.execute(
            UpdateComponent(
                UpdateComponent.Variables(
                    id = componentId,
                    description = description,
                    template = templateId.getOrNull(),
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to update component with name '${component.name}' (ID=$componentId): $error"))
        }

        return Either.Right(componentResult.updateComponent.component.id)
    }

    private suspend fun createComponentVersion(componentId: ID, component: Component): Either<GropiusExportFailure, ID> {
        val version = when (component) {
            is Component.ExternalComponent -> null
            is Component.InternalComponent -> component.version?.value
        }

        val result = graphQLClient.execute(
            CreateComponentVersion(
                CreateComponentVersion.Variables(
                    component = componentId,
                    description = version?.let { "v$it" } ?: "-",
                    name = component.name.value,
                    version = version ?: "-",
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to create the component version for component with name '${component.name}' (ID=$componentId): $error"))
        }

        return Either.Right(result.createComponentVersion.componentVersion.id)
    }

    private suspend fun getComponentVersionOrNull(componentId: ID): Either<GropiusExportFailure, ID?> {
        val result = graphQLClient.execute(
            GetComponentVersionById(
                GetComponentVersionById.Variables(
                    id = componentId
                )
            )
        ).getOrElse { error ->
            return Either.Left(GropiusExportFailure("failed to get component version '$componentId': $error"))
        }
        return Either.Right(result.components.nodes.first().versions.nodes.firstOrNull()?.id)
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

    private suspend fun addRelations(communications: Set<Communication>, gropiusComponents: Set<GropiusComponent>): Either<GropiusExportFailure, Unit> {
        for (communication in communications) {
            val start = gropiusComponents.find { it.component.name == communication.source.componentName }?.componentVersionId
            val end = gropiusComponents.find { it.component.name == communication.target.componentName }?.componentVersionId
            if (start == null || end == null) {
                log.warn { "No relation can be added. Start: ${start?.value} End: ${end?.value}" }
            } else {
                graphQLClient.execute(
                    CreateRelation(
                        CreateRelation.Variables(
                            start = start.value,
                            end = end.value,
                            relTemplateId = "853e1d82-7f62-45ea-8cbe-797c2a2f35f6" // General Relation Template
                        )
                    )
                ).getOrElse { error ->
                    return Either.Left(GropiusExportFailure("failed to create the relation from '${start.value}' to '${end.value}': $error"))
                }
            }
        }

        return Either.Right(Unit)
    }
}
