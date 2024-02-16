package de.unistuttgart.iste.sqa.clara.export.gropius

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import de.unistuttgart.iste.sqa.clara.api.export.ExportFailure
import de.unistuttgart.iste.sqa.clara.api.export.Exporter
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import de.unistuttgart.iste.sqa.clara.api.model.Component
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GraphQLClient
import de.unistuttgart.iste.sqa.clara.export.gropius.graphql.GropiusGraphQLClient
import de.unistuttgart.iste.sqa.gropius.CreateComponent
import de.unistuttgart.iste.sqa.gropius.GetProjectById
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

        val result = runBlocking {
            // TODO: run the correct queries and mutations
            //graphQLClient.execute(DeleteRelation(DeleteRelation.Variables(id = "")))
            graphQLClient.execute(
                CreateComponent(
                    CreateComponent.Variables(
                        description = "asdf",
                        name = "asdf2name",
                        template = "cdfcfe50-3602-4ace-8d85-d8bccfe15cde"
                    )
                )
            )
        }.getOrElse {
            return Some(ExportFailure("Gropius: errors while executing a GraphQL request: $it"))
        }

        println(result)

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
//
//    fun addDatasetComponents(components: Set<Component>) {
//        val resultComponents = runBlocking {
//            graphQLClient.execute(GetComponents())
//        }
//
//        val createdComponentsIdList: MutableMap<Component, String> = mutableMapOf()
//
//        // check if exact component already exists
//        for (component in components) {
//            /*
//            // TODO: decide if and how detected components are congruent with db entries (match by name, ip, id, ...)
//
//            var tmpId = ""
//            // TODO: outsource component mapping to a config file or component object
//            when (component) {
//                is Component.Internal.Pod -> tmpId = "3fdb8e40-26f7-4621-82fa-394a77b55a88"
//                is Component.Internal.Service -> tmpId = "3fdb8e40-26f7-4621-82fa-394a77b55a88"
//                is Component.External -> TODO()
//            }
//            */
//
//            // TODO: change query to search for object in db
//            val searchResult = resultComponents.data?.components?.nodes?.find { it.id == "TODO ID" }
//            if (searchResult == null) {
//
//                val resultComponent = runBlocking {
//                    graphQLClient.execute(CreateComponent(component))
//                }
//
//                // TODO: is this the correct way of detecting unsuccessful mutations???
//                val idResult = resultComponent.data?.createComponent?.component?.id;
//                if (idResult != null) {
//                    createdComponentsIdList[component] = idResult
//                }
//            } else {
//                createdComponentsIdList[component] = searchResult.id
//            }
//        }
//    }
}
