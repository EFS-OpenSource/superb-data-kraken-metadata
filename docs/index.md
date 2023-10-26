# Metadata Service


The Metadata service is a service that provides functionality for indexing in opensearch.


### Installing


Execute the following steps to set up your local environment for development and testing:

- Clone the repository

## Usage


Commands that are required in order to use the service.

- <code>mvn package</code> to build the service
- <code>mvn test</code> to execute the tests
- <code>mvn spring-boot:run</code> to run a spring service
- <code>docker build</code> for building the docker container

## Deployment


For deployment push the service to Azure DevOps, the pipeline will start automatically for development and master
branches. For feature branches, please start it manually.
The deployment manifest is [azure-pipeline-template.yml](azure-pipeline-template.yml).


### Build/Deployment steps:

- Build and Push: an image is built using the [Dockerfile](Dockerfile) and pushed to the corresponding ACR (SDK or
  AICloud).
- Deployment: kubernetes manifests are deployed to the corresponding AKS (SDK or AICloud):
    - [config-map.yml](kubernetes/config-map.yml) writes the spring boot configuration application.yml as a config map
    - [rbac.yml](kubernetes/rbac.yml) gives permission for backend namespace
    - [deployment.yml](kubernetes/deployment.yml)  yields the k8 deployment "metadata", i.e. describes the desired state
      for Pods and ReplicaSets
    - [service.yml](kubernetes/service.yml) yields the corresponding k8 service "metadata-service", i.e. an abstract way
      to expose an application running on a set of Pods as a network service.
    - [ingress.yml](kubernetes/ingress.yml) yields the ingress "metadata" to the service, i.e. manages external http
      access to the service in the cluster via the public
      IP https://efs-aicloud.westeurope.cloudapp.azure.com/sdk-frontend/

### Service connections


For setting up the pipelines, the following service connections are needed in Azure Devops -> Project Settings:


#### Docker Registry service connection

- for SDK tenant: sc-efs-sdk-acrsdk (type: Azure Container Registry)

- for AICloud tenant: sc-efs-sdk-acraicloud (type: others)
    - docker registry: https://acraicloud.azurecr.io/
    - docker id: acraicloud
    - docker password: obtained from portal -> ACR -> access keys -> enable admin user -> copy password

#### Kubernetes service connection

- for SDK tenant: sc-efs-sdk-aks-sdk_devops
- for AICloud tenant: sc-efs-sdk-aks-aicloud_devops

Both are of type Service Account and have the following parameters

- server url: obtained (as described in Azure DevOps) from
  ```bash
  kubectl config view --minify -o jsonpath={.clusters[0].cluster.server}
  ```
- secret: obtained from
    ```bash
  kubectl get serviceAccounts <service-account-name> -n <namespace> -o=jsonpath={.secrets[*].name}
  ```
  where namespace is default and the service account is e.g. appreg-aicloud-aks-main.

---


### Pipeline Variables


the following pipeline variables are required:

| name                               | example                       |
|------------------------------------|-------------------------------| 
| $(dockerRegistryServiceConnection) | sc-efs-sdk-acraicloud         |
| kubernetesServiceConnection        | sc-efs-sdk-aks-aicloud_devops |
| environment                        | aicloud                       |
| opensearch-service                 | opensearch-cluster-client     |

The container registry service connection is established during pipeline creation.


## Built With


Tools used for building. Example:

- Maven v3.6.3 (see this [Link](https://maven.apache.org/))

## Contributing


See the [Contribution Guide](CONTRIBUTING.md).


## Changelog


See the [Changelog](CHANGELOG.md).


## Documentation


### OpenSearch-Context


The Metadata service is responsible for managing OpenSearch context resources for organizations and spaces, including tenants, roles, role mappings,
specific indices, and aliases.
OpenSearch roles are linked via rolesmappings to Keycloak roles.
This approach ensures that Keycloak, as the Identity and Access Management (IAM) service of the SDK, is responsible for authorizing user access to specific
resources. By creating a tenant and roles for each organization and space, users are granted appropriate permissions and security is maintained across the
system.

The Organizationmanager calls the Metadataservice directly via REST API and provides all relevant
information (via `com.efs.sdk.common.domain.dto.OrganizationContextDTO`) to create, update and delete OpenSearch contexts (see sequence diagram)

![Orgamanager Connection](docs/images/organizationmanager-connection.svg)


#### Organization


For every organization, an OpenSearch **tenant** with the same name is automatically created, along with **roles** that define permissions within the tenant.
Additionally, **role mappings** are established to map the OpenSearch roles to their corresponding Keycloak roles.

**Role Overview Organization (Keycloak - OpenSearch)**

| Keycloak Role                                                | OpenSearch Role                        | OpenSearch Tenant permission        |
|--------------------------------------------------------------|----------------------------------------|-------------------------------------|
| ```org_<organizationName>_access```                          | ```<organizationName>_access```        | read (```kibana_all_read```)        |
| ```org_<organizationName>_trustee/admin```                   | ```<organizationName>_trustee/admin``` | read,write (```kibana_all_write```) |
| ```org_all_public``` + organization confidentiality = public | ```<organizationName>_public```        | read (```kibana_all_read```)        |

#### Space


For every space, an OpenSearch **tenant** with name ```<organizationName>_<spaceName>``` is automatically created, OpenSearch roles (defining permissions within
the tenant and to the index) and rolesmappings to map the OpenSearch roles to their corresponding Keycloak roles are created.

A new index with the form of ```<organizationName>_<spaceName>_measurements```, and
alias ```measurements``` will be created, if the space has the capability "metadata".

The service will create an index according to the pattern ```<org>_<spc>_measurements``` with an alias
```measurements```. This alias allows users to search in all indices one has permission to by simply calling
```GET /measurements/_search```, rather than specifying a precise
index-name ```GET /<org.name>_<spc. name>_measurements/_search```.

![Index-Hierarchy](docs/images/index-hierarchy.svg)

In order to enable authorization to an index and tenant via OAuth roles, opensearch-roles must be created. These OpenSearch roles contain permissions that
define the level
of access for users with that role (e.g. ```read``` or ```crud```).
Rolesmappings form the link between opensearch-roles and OAuth-roles. In the case of SDK's role/rights concept, both organization and space rights must be
available. Therefore, roles are defined in the rolesmapping as```and_backend_roles```.

**Role Overview Space (Keycloak - OpenSearch)**

| Keycloak (Space) Role                                 | OpenSearch Role                                    | Index pattern                          | Index permission                                                                                    | OpenSearch Tenant pattern            | OpenSearch Tenant permission        |
|-------------------------------------------------------|----------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------|--------------------------------------|-------------------------------------|
| ```<organizationName>_<spaceName>_user/supplier```    | ```<organizationName>_<spaceName>_user/supplier``` | ```<organizationName>_<spaceName>_*``` | read <br> indices:data/read/scroll <br> indices:admin/mappings/get                                  | ```<organizationName>_<spaceName>``` | read (```kibana_all_read```)        |
| ```<organizationName>_<spaceName>_trustee```          | ```<organizationName>_<spaceName>_trustee```       | ```<organizationName>_<spaceName>_*``` | crud <br>  indices:data/read/scroll <br> indices:admin/mappings/get <br> indices:admin/mappings/put | ```<organizationName>_<spaceName>``` | read,write (```kibana_all_write```) |
| ```spc_all_public``` + space confidentiality = public | ```<organizationName>_<spaceName>_all_public```    | ```<organizationName>_<spaceName>_*``` | read <br> indices:data/read/scroll <br> indices:admin/mappings/get                                  | ```<organizationName>_<spaceName>``` | read (```kibana_all_read```)        |

Further reading on permissions, see [Default action groups](https://opensearch.org/docs/latest/security/access-control/default-action-groups/).


### Application-Indices


### Indexing


This service provides functionality to index metadata.

```POST /metadata/v1.0/index```

**Parameters**

| Type | Name | Description |
|------|------|-------------|
| Body |      | metadata    | 

The service extracts this information and stores the metadata in your opensearch-instance (index named `<org>_<spc>_measurements`). When all is
done, a message of the form

```
{
  "space": "<A_SPACE_NAME>",
  "organization": "<AN_ORGANIZATION_NAME>",
  "rootDir": "<A_ROOT_DIRECTORY_NAME>",
  "uuid": "<AN_UUID>"
}
```

will be published to the topic 'indexing-done' (as configured via ```metadata.topics.indexing-done-topic```).

```PUT /metadata/v1.0/index```

**Parameters**

| Type         | Name         | Description                                                      |
|--------------|--------------|------------------------------------------------------------------|
| RequestParam | organization | Name of the organization                                         |
| RequestParam | space        | Name of the space                                                |
| RequestParam | docid        | ID of the document to update                                     |
| Body         |              | metadata and massdata that needs to be added to the current file |

This service gets a JSON with two attributes - `metadata` and `massdata` (if missing no changes will be applied).
Metadata is an object, the attribute won't overwrite already existing sub-attributes in the old document.
Massdata is an array, the array adds new file to the existing document if there is no element with the exact same data. When all is
done, a message of the form

```
{
  "space": "<A_SPACE_NAME>",
  "organization": "<AN_ORGANIZATION_NAME>",
  "rootDir": "<A_ROOT_DIRECTORY_NAME>",
  "uuid": "<AN_UUID>"
}
```

will be published to the topic 'metadata-update' (as configured via ```metadata.topics.metadata-update-topic```).



### TODO further functionality


### Swagger


The API is documented using [Swagger](https://swagger.io/) (OpenAPI Specification). Developers may
use [Swagger UI](https://swagger.io/tools/swagger-ui/) to visualize and interact with the API's resources
at `http(s)://(host.domain:port)/metadata/swagger-ui/index.html`.


### Configuration


This documentation should only serve for non-well-known-spring-boot-properties.


#### Springfox-configuration

* ```apidoc.oauth2.access-token-uri``` token-endpoint of your OAuth-provider
* ```apidoc.oauth2.user-authorization-uri``` authorization-endpoint of your OAuth-provider
* ```apidoc.description``` description of this service
* ```apidoc.hostname``` hostname the service runs at (unread in Springfox 3.0.0)
* ```apidoc.protocol``` protocol of service (unread in Springfox 3.0.0)

#### metadata-configuration

* ```metadata.opensearch.index``` name of your opensearch-index
* ```metadata.opensearch.url``` url of your opensearch-instance
* ```metadata.opensearch.user``` name of your opensearch-user
* ```metadata.opensearch.password``` password of your opensearch-user
* ```metadata.topics.indexing-done-topic``` topic which should trigger indexing

In order to generate access-tokens in the context of the uploading user the following
properties are provided:

* ```metadata.auth.client-id``` The client-id of the confidential client
* ```metadata.auth.client-secret``` The client-secret of the confidential client


To get started, you need to provide the following:

* Setup opensearch
* make sure that the user has all necessary roles. In particular, it needs the role SDK_ADMIN.
* Provide Kubernetes-Secret for meta-json-schema (this secret will in future be created by a pipeline) to check jsons
  against this json schema.
  <code> kubectl create secret generic meta-json-schema --from-file=metadata.schema.meta-json-schema=meta-schema.json -n
  backend </code>

#### Local setup


For a local setup, you have to provide:

* opensearch:
    * available on localhost:9200
    * username: "admin"
    * password: "admin"

Please note that the placeholders in application-local.yml (username, password) must be set manually (and locally)!
Please also consider changing the json schema in bootstrap-local.yml, if necessary.



## TODO


Currently, the documentation is located in usual files like `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md` and `LICENSE.md` inside the root folder of the
repository. That folder is not processed by MkDocs. To build the technical documentation for MkDocs we could follow these steps:

- Move the documentation to Markdown files inside the `docs` folder.
- Build a proper folder/file structure in `docs` and update the navigation in `mkdocs.yaml`.
- Keep the usual files like `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md` and `LICENSE.md` inside the root folder of the repository (developers expect them to
  be there, especially in open source projects), but keep them short/generic and just refer to the documentation in the `docs` folder.