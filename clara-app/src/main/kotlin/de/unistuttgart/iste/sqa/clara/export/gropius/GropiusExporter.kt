package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.*
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.export.onFailure
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
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
    ) {

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

    override fun export(components: Set<Component>, communications: Set<Communication>): Option<ExportFailure> {
        log.info { "Export to Gropius ..." }

        // 2. check templates
        // checkDatasetTemplates()
        //
        // 3. check for existing components
        //    - generic components e.g. postgresql container may already exist
        //
        // 4. create component versions
        //    - based on what? --> get version code from clara?
        //
        // 5. compare graph to local data
        //    - strategy: update db? / start completely from scratch
        //
        // 6. upload dataset

        checkProject().onFailure { return it }

        checkDatasetTemplates().onFailure { return it }

        val dataSetComponents = addDatasetComponents(components)
        addRelations(communications, dataSetComponents)

        log.info { "Done exporting to Gropius" }

        return None
    }

    private fun checkProject(): Option<GropiusExportFailure> {
        val projectResult = runBlocking {
            graphQLClient.execute(GetProjectById(GetProjectById.Variables(id = config.projectId)))
        }

        return when (projectResult) {
            is Either.Left -> Some(GropiusExportFailure("Cannot validate the existence of the project with the ID ${config.projectId} (${projectResult.value})"))
            is Either.Right -> if (projectResult.value.projects.nodes.isEmpty()) Some(GropiusExportFailure("The project with the ID ${config.projectId} does not exist!")) else None
        }
    }

    private fun checkDatasetTemplates(): Option<GropiusExportFailure> {
        // TODO: are the component scoped under the project ID?
        val componentTemplatesResult = runBlocking { graphQLClient.execute(GetAllComponentTemplates()) }
        val relationTemplatesResult = runBlocking { graphQLClient.execute(GetAllRelationTemplates()) }

        val componentTemplates = componentTemplatesResult
            .map { result -> result.componentTemplates.nodes }
            .getOrElse { error -> return Some(GropiusExportFailure("Failed to query all component templates ($error)")) }

        val relationTemplates = relationTemplatesResult
            .map { result -> result.relationTemplates.nodes }
            .getOrElse { error -> return Some(GropiusExportFailure("Failed to query all relation templates ($error)")) }

        // TODO: check if required templates exist (see clara config)

        val neededComponentTemplates = emptyList<String>()
        val neededRelationTemplates = emptyList<String>()

        // TODO: generate required templates from
        /*
        val resultAddComponentTemplates = runBlocking {  // does batch processing work here?
            for (tmplComponent in neededComponentTemplates) {
                graphQLClient.execute(CreateComponentTemplate(tmplComponent))
            }

            for (tmplRelation in neededRelationTemplates) {
                graphQLClient.execute(CreateRelationTemplate(tmplRelation))
            }
        }
         */

        // TODO: error handling

        return None
    }

    private fun addDatasetComponents(components: Set<Component>): Set<GropiusComponent> {
        val resultComponents = runBlocking {
            graphQLClient.execute(GetAllComponents()).getOrElse {
                throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
            }
        }

        val createdGropiusComponents = mutableSetOf<GropiusComponent>()

        // check if exact component already exists -> TODO: this should be configurable. Modes for existing components could be: fail, override, ...
        for (component in components) {

            // TODO: decide if and how detected components are congruent with db entries (match by name, ip, id, ...)

            // TODO: change query to search for object in db
            val searchResult = resultComponents.components.nodes.find { it.name == component.name.value }
            if (searchResult == null) {
                val componentResult = runBlocking {
                    val (description, template) = when (component) {
                        is Component.InternalComponent -> Pair("IP-address: ${component.ipAddress?.value ?: "unknown"}", "ed79a792-7cd3-40ba-8b75-42bac1fbbede") // microservice template
                        is Component.ExternalComponent -> Pair("Domain: ${component.domain.value}", "df765fb5-8085-414e-af4b-07cd97161d21") // general template
                    }
                    graphQLClient.execute(
                        CreateComponent(
                            CreateComponent.Variables(
                                description = description,
                                name = component.name.value,
                                template = template,
                                repositoryURL = "https://example.org" // in the future we might want to add the repo link gathered from the SBOM here
                            )
                        )
                    )
                }.getOrElse {
                    // TODO: is this the correct way of detecting unsuccessful mutations???
                    throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                // Add version to component
                val versionResult = runBlocking {
                    graphQLClient.execute(
                        CreateComponentVersion(
                            CreateComponentVersion.Variables(
                                component = componentResult.createComponent.component.id,
                                description = "v1.0", // TODO versioning
                                name = component.name.value,
                                version = "1.0",
                            )
                        )
                    )
                }.getOrElse {
                    throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                val componentId = GropiusComponent.ComponentId(componentResult.createComponent.component.id)
                val versionId = GropiusComponent.ComponentVersionId(versionResult.createComponentVersion.componentVersion.id)
                createdGropiusComponents.add(GropiusComponent(component, componentId, versionId))

                // Add componentVersion to project
                runBlocking {
                    graphQLClient.execute(
                        AddComponentVersionToProject(
                            AddComponentVersionToProject.Variables(
                                project = config.projectId,
                                componentVersion = versionResult.createComponentVersion.componentVersion.id
                            )
                        )
                    )
                }.getOrElse {
                    throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
                }
            } else {
                val versionResult = runBlocking {
                    graphQLClient.execute(
                        GetComponentVersionById(
                            GetComponentVersionById.Variables(
                                id = searchResult.id
                            )
                        )
                    )
                }.getOrElse {
                    throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                createdGropiusComponents.add(
                    GropiusComponent(
                        component = component,
                        componentId = GropiusComponent.ComponentId(searchResult.id),
                        componentVersionId = GropiusComponent.ComponentVersionId(versionResult.components.nodes.first().versions.nodes.first().id)
                    )
                )
            }
        }
        return createdGropiusComponents
    }

    private fun addRelations(communications: Set<Communication>, gropiusComponents: Set<GropiusComponent>) {
        for (communication in communications) {
            val start = gropiusComponents.find { it.component.name == communication.source.componentName }?.componentVersionId ?: throw UnsupportedOperationException("TODO")
            val end = gropiusComponents.find { it.component.name == communication.target.componentName }?.componentVersionId ?: throw UnsupportedOperationException("TODO")
            runBlocking {
                graphQLClient.execute(
                    CreateRelation(
                        CreateRelation.Variables(
                            start = start.value,
                            end = end.value,
                            relTemplateId = "7bb43192-715b-44db-af2d-3eaba6f846ea" // General Relation Template
                        )
                    )
                )
            }.getOrElse {
                throw UnsupportedOperationException("errors while executing a GraphQL request: $it") // FIXME proper handling
            }
        }
    }
}
