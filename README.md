# WildFly Sample

A small Java web application using **WildFly** as its application server,
**Jakarta EE 10** (REST, CDI, and JSON-B) as its framework APIs, **JUnit 5**
for tests, and **Maven** for builds.

## Requirements

- JDK 21, with `JAVA_HOME` pointing to the JDK directory.
- The included Maven wrapper downloads Maven 3.9.9 automatically. A separate
  Maven installation is optional.

WildFly supplies the Jakarta EE implementation at runtime, so the Jakarta API
dependency is `provided` and is not bundled into the WAR. The Maven plugin
downloads WildFly 35.0.1.Final for the local run command; no separate server
installation is needed. The first build/run requires an internet connection.
Initial server provisioning can take several minutes; subsequent runs reuse
the server in `target/server` unless `clean` removes it.

The examples use a Linux shell. If Maven is already installed, you can replace
`sh ./mvnw` with `mvn`.

## Build and test

From this project directory:

```bash
sh ./mvnw clean verify
```

This runs the JUnit tests and creates `target/wildfly-sample.war`.
To run only the tests:

```bash
sh ./mvnw test
```

The tests cover default greetings, named greetings, whitespace handling, and
resource delegation. They are fast unit tests and do not start WildFly.

## Run locally

```bash
sh ./mvnw wildfly:run
```

This starts WildFly and deploys the WAR. Keep the terminal open; press `Ctrl+C`
to stop it. Ports 8080 (HTTP) and 9990 (management) must be available.

On a slow machine, if the plugin reports that the server failed to start in
60 seconds, allow more time:

```bash
sh ./mvnw "-Dwildfly.startupTimeout=300" "-Dwildfly.timeout=300" wildfly:run
```

Open <http://localhost:8080/wildfly-sample/> for the landing page.

### Try the REST API

```bash
curl "http://localhost:8080/wildfly-sample/api/greeting"
curl "http://localhost:8080/wildfly-sample/api/greeting?name=Alice"
```

Responses:

```json
{"message":"Hello, World!"}
```

```json
{"message":"Hello, Alice!"}
```

Missing, empty, or whitespace-only names use `World`; surrounding whitespace
is removed from nonblank names. JSON-B serializes the response, including
escaping characters in user-supplied names.

## Build and run a container

Install Docker Engine, then run these commands from the project directory:

```bash
docker build -t wildfly-sample:1.0 .
docker run --rm --name wildfly-sample -p 127.0.0.1:8080:8080 wildfly-sample:1.0
```

The multi-stage [Dockerfile](Dockerfile) builds the application and runs all
JUnit tests with Maven 3.9.9 and JDK 21, then copies only the WAR into the
WildFly 35.0.1.Final / JDK 21 runtime image. You do not need Java, Maven, or a
prebuilt WAR installed on the host. The [.dockerignore](.dockerignore)
excludes local build output and editor metadata from the build context.

WildFly runs as the image's non-root `jboss` user and listens on port 8080.
The command publishes HTTP only to the host's loopback interface; the
management port is not published. Wait for deployment to finish, then open
<http://localhost:8080/wildfly-sample/> or test:

```bash
curl "http://localhost:8080/wildfly-sample/api/greeting?name=Alice"
```

To stop the container from another terminal:

```bash
docker stop wildfly-sample
```

`--rm` removes the container after it stops; the image remains available.
The first build requires access to Docker Hub, Quay, and Maven Central.
Corporate proxy CA certificates must also be trusted inside the build image.

### Container builds behind an HTTPS-inspecting proxy

If Maven fails with `PKIX path building failed` during `docker build`, the
build container's Java trust store does not trust the certificate chain
presented for Maven Central. This often happens when a corporate proxy
replaces the site's certificate. Containers do not inherit the host's
trusted certificates.

Obtain your organization's approved proxy CA certificate from your IT team
in **Base-64 encoded X.509 (.CRT or .CER)** format. Verify its fingerprint
with IT; use the CA certificate, not Maven Central's leaf certificate, and
never export a private key. Keep the certificate outside the project.

Supply that public certificate through a BuildKit secret:

```bash
docker build --secret "id=maven_ca,src=/path/to/corporate-root-ca.crt" -t wildfly-sample:1.0 .
```

The Dockerfile imports the optional certificate into the build-stage JDK's
trust store before running Maven. Invalid certificates fail the build.
TLS verification remains enabled, and the certificate/trust store is not
copied into the final WildFly image. Normal builds without this secret
continue to use the JDK's default trusted CAs. Recent Docker versions use
BuildKit by default.

After changing the certificate, add `--no-cache` to rebuild: changing secret
contents alone does not invalidate Docker's build cache.

This fixes Maven's in-container HTTPS trust. If pulling a base image fails
with a certificate error, Docker's own proxy/CA configuration must be fixed
separately. Runtime HTTPS calls from WildFly also need their own trust
configuration if they pass through the proxy.

## Deploy to an existing WildFly server

Start a Jakarta EE 10-compatible WildFly server with its local management
endpoint on port 9990, then run:

```bash
sh ./mvnw clean package wildfly:deploy
```

The default connection uses local management authentication. A remote or
secured server requires the Maven plugin's management connection settings.
Do not store management passwords in this project.

To undeploy:

```bash
sh ./mvnw wildfly:undeploy
```

## OpenShift Pipelines

If you want to build, test, and publish the application image from OpenShift,
see [openshift/pipelines/README.md](./openshift/pipelines/README.md). That
example uses a manual Tekton PipelineRun, the OpenShift internal registry, and
an administrator-installed non-root Buildah SCC.

## Linux certificate troubleshooting

If Maven reports `PKIX path building failed` on a corporate network, make sure
your organization's CA certificate is trusted by the JDK. Install the CA into
your Linux distribution's system trust store and, if needed, follow your JDK
vendor's instructions for importing system certificates into Java's trust
store. On Debian or Ubuntu, for example:

```bash
sudo install -m 0644 corporate-root-ca.crt /usr/local/share/ca-certificates/corporate-root-ca.crt
sudo update-ca-certificates
```

Retry the build after updating the trust store. This keeps TLS certificate
verification enabled; do not disable verification to work around certificate
errors.

## Project layout

```text
pom.xml
Dockerfile                     Multi-stage Maven build and WildFly runtime
.dockerignore                  Container build context exclusions
mvnw                            Maven wrapper for Linux and macOS
.mvn/wrapper                   Pinned Maven distribution
openshift/
  container/Dockerfile         Runtime image used by OpenShift Pipelines
  pipelines/                   Tekton tasks, pipeline, and run manifests
src
  main
    java/com/example/wildfly
      RestApplication.java      REST base path: /api
      GreetingResource.java     GET /greeting
      GreetingService.java      Greeting logic, injected using CDI
      Greeting.java             JSON response
    webapp
      index.html                Landing page
      WEB-INF/beans.xml         CDI discovery configuration
  test
    java/com/example/wildfly
      GreetingServiceTest.java
      GreetingResourceTest.java
```

This is a local-development learning sample, not a hardened production
deployment. For WildFly background and configuration, see the
[WildFly 35 documentation](https://docs.wildfly.org/35/).
