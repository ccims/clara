# Gropius Export

Gropius is an open-source cross-component issue management system for component-based architectures.
In order to enable managing cross-component dependencies, users can model component-based software architectures in a Gropius project, e.g. via the API.
For more details on Gropius visit the [GitHub Page](https://github.com/ccims).

For configuration options of the export please check out the [configurations](../../configuration/index.md) page.

### Data Model
The data model of Gropius consists of components which can be specified with templates as well as relations
between those components, also configurable via templates.
A component must have a component and a repository-URL in order to be added to a project, which resembles an architecture.

CLARA components are mapped to Gropius-components like this:
| **CLARA Metamodel**| **Gropius Metamodel** |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| InternalComponent | Component |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Name | &nbsp;&nbsp;&nbsp;&nbsp;Component.Name |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.IpAddress | &nbsp;&nbsp;&nbsp;&nbsp;Component.Description |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Version | &nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentVersion |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Namespace | &nbsp;&nbsp;&nbsp;&nbsp;*MISSING* |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Endpoints | &nbsp;&nbsp;&nbsp;&nbsp;*MISSING* (Note, that Gropius is capable of modeling interfaces, yet due to a lack of time this is not performed in the current work.) |
| &nbsp;&nbsp;&nbsp;&nbsp;*MISSING* | &nbsp;&nbsp;&nbsp;&nbsp;Component.RepositoryURL (Example URL) |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Type | &nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentTemplate |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type.Database| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Database Temp. |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type.Microservice | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Microservice Temp. |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Type.Messaging | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Messaging Temp.|
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Base Component Temp. |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Libraries | &nbsp;&nbsp;&nbsp;&nbsp;Components |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Library.Version | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentVersion |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Library.Name | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Component.Name |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Library.Name | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Component.Description |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;*MISSING* | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentTemplate (Library Temp.) |
| &nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Library | &nbsp;&nbsp;&nbsp;&nbsp;Relation |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;InternalComponent.Version | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Relation.Start|
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Library.Version | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Relation.End |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;*MISSING* | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Relation.RelationTemplate (Includes Temp.) |
| ExternalComponent | Component |
| &nbsp;&nbsp;&nbsp;&nbsp;ExternalComponent.Name | &nbsp;&nbsp;&nbsp;&nbsp;Component.Name |
| &nbsp;&nbsp;&nbsp;&nbsp;ExternalComponent.Domain | &nbsp;&nbsp;&nbsp;&nbsp;Component.Description |
| &nbsp;&nbsp;&nbsp;&nbsp;ExternalComponent.Type | &nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentTemplate (Misc Temp.) |
| &nbsp;&nbsp;&nbsp;&nbsp;ExternalComponent.Version | &nbsp;&nbsp;&nbsp;&nbsp;Component.ComponentVersion |
| Communication | Relation |
| &nbsp;&nbsp;&nbsp;&nbsp;Communication.Source.Version | &nbsp;&nbsp;&nbsp;&nbsp;Relation.Start |
| &nbsp;&nbsp;&nbsp;&nbsp;Communication.Target.Version | &nbsp;&nbsp;&nbsp;&nbsp;Relation.End |
| &nbsp;&nbsp;&nbsp;&nbsp;*MISSING* | &nbsp;&nbsp;&nbsp;&nbsp;Relation.RelationTemplate (Calls Temp.) |


The Gropius GraphQL API is utilized by CLARA in order to export the recovered architectures into a Gropius project.





### Export Flow
The export works sketched like this based on the respective configuration:

- for all components recovered by CLARA:
  - delete or update component
  - create or update component version
  - add component version to project
- add relations for all components 

For all CRUD operations there are predefined GraphQL queries which are transformed into Kotlin Models using this [GraphQl gradle plugin](https://mvnrepository.com/artifact/com.expediagroup.graphql/com.expediagroup.graphql.gradle.plugin) 
and executed using this [GraphQL Kotlin Spring client](https://mvnrepository.com/artifact/com.expediagroup/graphql-kotlin-spring-client).
The GraphQL queries are located in the clara-graphql directory.