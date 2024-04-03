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

TODO when templates are finalized.

The Gropius GraphQL API is utilized by CLARA in order to export the recovered architectures into a Gropius project.





### Export Flow
The export works sketched like this based on the respective configuration:
- TODO TEMPLATES
- for all components recovered by CLARA:
  - delete or update component
  - create or update component version
  - add component version to project
- add relations for all components 

For all CRUD operations there are predefined GraphQL queries which are transformed into Kotlin Models using this [GraphQl gradle plugin](https://mvnrepository.com/artifact/com.expediagroup.graphql/com.expediagroup.graphql.gradle.plugin) 
and executed using this [GraphQL Kotlin Spring client](https://mvnrepository.com/artifact/com.expediagroup/graphql-kotlin-spring-client).
The GraphQL queries are located in the clara-graphql directory.