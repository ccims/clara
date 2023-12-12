# DNS

CLARA can analyze the logs of CoreDNS (the default Kubernetes DNS server) to discover communication of components via DNS queries.
For that feature to work correctly, it is crucial that the DNS server is configured to log DNS queries by enabling the `log` plugin.

```yaml title="An exampple ConfigMap for CoreDNS with the 'log' plugin enabled" hl_lines="9"
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        log
        errors
        health
        ready
        kubernetes cluster.local in-addr.arpa ip6.arpa {
          pods insecure
          fallthrough in-addr.arpa ip6.arpa
        }
        prometheus :9153
        forward . /etc/resolv.conf
        cache 30
        loop
        reload
        loadbalance
    }
```

## Managed Kubernetes cluster

!!! warning "Using a managed Kubernetes cluster from a service provider"
    When using a managed cluster from a service provider, changes to core components of Kubernetes might be not allowed directly.
    Please consult the documentation of your respective provider.

### DigitalOcean

For DigitalOcean, the correct way of enabling logging is to create a [special ConfigMap](https://docs.digitalocean.com/products/kubernetes/how-to/customize-coredns/):

``` yaml title="ConfigMap to activate query logging for CoreDNS in a Kubernetes cluster managed by DigitalOcean"
--8<-- "content/aggregation/platforms/kubernetes/dns/digital-ocean.coredns-override.configmap.yml"
```

## DNS debugging

As described in the [Kubernetes Documentation](https://kubernetes.io/docs/tasks/administer-cluster/dns-debugging-resolution/), you can use dnsutils to debug DNS resolution.
For CLARA, this is also a simple way of creating DNS queries explicitly and checking if CLARA detects the communication.
Just create a dnsutils-pod with the following manifest:

``` yaml
--8<-- "content/aggregation/platforms/kubernetes/dns/dnsutils.pod.yml"
```

Then you can use the following command to execute DNS queries:

```shell linenums="0"
kubectl exec -it dnsutils -n default -- nslookup google.com
```

Execute the following command to check the DNS server logs:

```shell linenums="0"
kubectl logs -l k8s-app=kube-dns -n kube-system
```
