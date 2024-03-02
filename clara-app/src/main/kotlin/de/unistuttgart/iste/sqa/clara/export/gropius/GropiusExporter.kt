package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.*
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GraphQLClient
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GropiusGraphQLClient
import de.unistuttgart.iste.sqa.gropius.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.lang.UnsupportedOperationException
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

        // 1. load configs
        //    - defaults
        //    - gropius project
        //    - credentials
        //
        // 2. check project
        //
        //
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

        val dataSetComponents = addDatasetComponents(components)
        addRelations(communications, dataSetComponents)

        log.info { "Done exporting to Gropius" }

        return None
    }

    suspend fun checkProject() {

        val projectResult = graphQLClient.execute(GetProjectById(GetProjectById.Variables(id = config.projectId)))
        // TODO: throw error, when found projects with if are length 0
    }

//    suspend fun checkDatasetTemplates() {
//        val resultComponents = graphQLClient.execute(GetComponentTemplates())
//        val resultRelations = runBlocking {
//            graphQLClient.execute(GetRelationTemplates())
//        }
//
//        // TODO: check if required templates exist (see clara config)
//
//        val neededComponentTemplates: List<Any> = emptyList()
//        val neededRelationTemplates: List<Any> = emptyList()
//
//        // TODO: generate required templates from
//        /*
//        val resultAddComponentTemplates = runBlocking {  // does batch processing work here?
//            for (tmplComponent in neededComponentTemplates) {
//                graphQLClient.execute(CreateComponentTemplate(tmplComponent))
//            }
//
//            for (tmplRelation in neededRelationTemplates) {
//                graphQLClient.execute(CreateRelationTemplate(tmplRelation))
//            }
//        }
//         */
//
//        // TODO: error handling
//    }

    private fun addDatasetComponents(components: Set<Component>): Set<GropiusComponent> {
        val resultComponents = runBlocking {
            graphQLClient.execute(GetAllComponents()).getOrElse {
                throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
            }
        }

        val createdComponentsIdList: MutableSet<GropiusComponent> = mutableSetOf()

        // check if exact component already exists
        for (component in components) {

            // TODO: decide if and how detected components are congruent with db entries (match by name, ip, id, ...)

            // TODO: change query to search for object in db
            val searchResult = resultComponents.components.nodes.find { it.name == component.name.value }
            if (searchResult == null) {
                val componentResult = runBlocking {
                    val (description, template) = when (component) {
                        is Component.InternalComponent -> Pair(component.ipAddress?.value ?: "nothing", "ed79a792-7cd3-40ba-8b75-42bac1fbbede") // microservice template
                        is Component.ExternalComponent -> Pair(component.domain.value, "df765fb5-8085-414e-af4b-07cd97161d21") // general template
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
                    throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                // Add version to component
                val versionResult = runBlocking {
                    graphQLClient.execute(
                        CreateComponentVersion(
                            CreateComponentVersion.Variables(
                                component = componentResult.createComponent.component.id,
                                description = "v1.0", // TODO versioning
                                name = "v1.0",
                                version = "v1.0",
                            )
                        )
                    )
                }.getOrElse {
                    throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                val componentId = GropiusComponent.ComponentId(componentResult.createComponent.component.id)
                val versionId = GropiusComponent.ComponentVersionId(versionResult.createComponentVersion.componentVersion.id)
                createdComponentsIdList.add(GropiusComponent(component, componentId, versionId))

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
                    throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
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
                    throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
                }

                createdComponentsIdList.add(
                    GropiusComponent(
                        component = component,
                        componentId = GropiusComponent.ComponentId(searchResult.id),
                        componentVersionId = GropiusComponent.ComponentVersionId(versionResult.components.nodes.first().versions.nodes.first().id)
                    )
                )
            }
        }
        return createdComponentsIdList
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
                throw UnsupportedOperationException("Gropius: errors while executing a GraphQL request: $it") // FIXME proper handling
            }
        }
    }
}

data class GropiusComponent(
    val component: Component,
    val componentId: ComponentId,
    val componentVersionId: ComponentVersionId,
) {

    @JvmInline
    value class ComponentId(val value: String)

    @JvmInline
    value class ComponentVersionId(val value: String)
}
