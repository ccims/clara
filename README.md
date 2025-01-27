### Cluster Architecture Recovery Assistant (CLARA) Artifact Review Helper

Dear Reviewer, thank you for taking time to review our CLARA framework. This document serves as helper for your review process and as documentation of the submitted artifact. Please note, that CLARA comes with a detailed documentation in the repository's GitHub pages.

## What is CLARA?
CLARA is a framework to recover the architecture of Kubernetes-deployed software systems and map them to a Gropius model. It combines multiple recovery methods to achieve as much accuracy as possible. Nevertheless, using a subset of the methods is possible if required but might lead to suboptimal results. Gropius is a cross-component issue management systems that manages issues with respect to their architectural dependencies and acts as a wrapper above traditional issue management systems such as GitHub or Jira. The objective of CLARA is to reduce the modeling effort for creating a Gropius model for existing software projects by recovering the architecture model.

## Where to find the artifact
There are three simple ways to retrieve our artifact: (1) take the submitted zip folder and extract everything, (2) download the GitHub release we created for the paper on https://github.com/ccims/clara/releases/tag/CLARA-paper, or (3) clone the GitHub repository (https://github.com/ccims/clara/tree/CLARA-paper). Otherwise, feel free to check out our archive on Zenodo: https://zenodo.org/records/14534069.

## Prerequisites
You will require (1) a Kubernetes cluster, e.g., minikube, or a remote one, (2) kube-api, (3) ktunnel, (4) syft, and (5) Java 21 and Gradle.
A detailed description of the prerequisites and steps can be found at https://ccims.github.io/clara/setup/.

## Elements of the artifact / replication package
Our artifact contains (1) the CLARA framework and (2) a version of the T2-Project reference architecture to recover. (3) Optionally, you can install Gropius via Docker Compose by following the CLARA documentation.
Using Gropius is not required as CLARA can also export the architecture using GraphViz.

## What to do
It is best to follow our documentation on CLARA's GitHub pages step by step: https://ccims.github.io/clara/. Please also note the "Validation" part that explains how to re-create our evaluation using the T2-Project as example architecture.

