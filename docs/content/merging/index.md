# Merging

Merging is applied as third step in the data pipeline, as shown in [concept](../concept/index.md).
Merging is mandatory, as the results of the different aggregations need to be merged into a homogenous data format to retrieve a holistic picture. 
Further, duplications are removed. For details on configuration possibilities see the [configurations](../configuration/index.md) page.

### Concept
The following concepts and data operations are applied in the merging step of CLARA:  

- [Base and Comparison Components](#base-and-comparison-components) 
- [Comparing](#comparing) 
- [Merging](#merging-1)
- [Dealing with Renamed Components](#dealing-with-renamed-components) 
- [Leftover Components](#leftover-components) 
- [Communications](#communications) 
- [Adjusting Messaging Communications](#adjusting-messaging-communications)

#### Base and Comparison Components
In CLARA the merging of two detected components by different aggregators is defined as merging a comparison object on top of the base object.   
In general, the components aggregated from the Kubernetes API are considered as the base component and OpenTelemetry components are considered as compare components.  
This is the case, because the Kubernetes API can be perceived as the ground truth about what is deployed in the cluster.

#### Comparing
In the comparison step for every Kubernetes component a matching OpenTelemetry component is searched.
The matching is currently only be done by the name.  
Thereby CLARA can be configured to match only equal names or also match if one name contains the other (e.g. cart-pod-12345 and cart).

#### Merging
In the merging step both component objects from Kubernetes and from OpenTelemetry are merged into a new final component object.
Thereby, the Kubernetes component is providing the service-name, Kubernetes namespace, IP-address, and if applicable the version.
The OpenTelemetry Component provides the endpoints and most likely the service type (e.g. database).

#### Dealing with Renamed Components
If a merged component was matched via a "contains"-pattern matching it is likely, that the final component has a different name then the OpenTelemetry component.
Thus, the relations discovered between OpenTelemetry components need to be adjusted to match the new naming.

#### Leftover Components
All components that could not be matched are simply mapped to a final component, with whatever attributes are available, to not lose any information.

#### Communications
Communications do not really have to be merged, as they are simply stacked upon each other in the exporter and do not contain any meta-information except source and target.

#### Adjusting Messaging Communications
Communications that are tagged as messaging communication, are also adjusted in the merger. CLARA can be configured to either show communications via a message broker
or filter out the message broker and show the communications between the communications directly.  
The latter can make it easier to understand the real communication flows of an application, especially if everything runs via a message broker.


