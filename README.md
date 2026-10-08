# WildFly JUnit Sample

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
the server in `target\server` unless `clean` removes it.

The examples use PowerShell on Windows. If Maven is already installed, you
can replace `.\mvnw.cmd` with `mvn`.

## Build and test

From this project directory:

```powershell
.\mvnw.cmd clean verify
```

This runs the JUnit tests and creates `target\wildfly-junit-sample.war`.
To run only the tests:

```powershell
.\mvnw.cmd test
```

The tests cover default greetings, named greetings, whitespace handling, and
resource delegation. They are fast unit tests and do not start WildFly.

## Run locally

```powershell
.\mvnw.cmd wildfly:run
```

This starts WildFly and deploys the WAR. Keep the terminal open; press `Ctrl+C`
to stop it. Ports 8080 (HTTP) and 9990 (management) must be available.

On a slow machine, if the plugin reports that the server failed to start in
60 seconds, allow more time:

```powershell
.\mvnw.cmd "-Dwildfly.startupTimeout=300" "-Dwildfly.timeout=300" wildfly:run
```

Open <http://localhost:8080/wildfly-junit-sample/> for the landing page.

### Try the REST API

```powershell
curl.exe "http://localhost:8080/wildfly-junit-sample/api/greeting"
curl.exe "http://localhost:8080/wildfly-junit-sample/api/greeting?name=Alice"
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

Install Docker with a running Linux-container engine (Docker Desktop on
Windows), then run these commands from the project directory:

```powershell
docker build -t wildfly-junit-sample:1.0 .
docker run --rm --name wildfly-junit-sample -p 127.0.0.1:8080:8080 wildfly-junit-sample:1.0
```

The multi-stage [Dockerfile](Dockerfile) builds the application and runs all
JUnit tests with Maven 3.9.9 and JDK 21, then copies only the WAR into the
WildFly 35.0.1.Final / JDK 21 runtime image. You do not need Java, Maven, or a
prebuilt WAR installed on the host. The [.dockerignore](.dockerignore)
excludes local build output and editor metadata from the build context.

WildFly runs as the image's non-root `jboss` user and listens on port 8080.
The command publishes HTTP only to the host's loopback interface; the
management port is not published. Wait for deployment to finish, then open
<http://localhost:8080/wildfly-junit-sample/> or test:

```powershell
curl.exe "http://localhost:8080/wildfly-junit-sample/api/greeting?name=Alice"
```

To stop the container from another terminal:

```powershell
docker stop wildfly-junit-sample
```

`--rm` removes the container after it stops; the image remains available.
The first build requires access to Docker Hub, Quay, and Maven Central.
Corporate proxy CA certificates must also be trusted inside the build image;
the Windows certificate-store workaround below applies only to host builds.

### Container builds behind an HTTPS-inspecting proxy

If Maven fails with `PKIX path building failed` during `docker build`, the
build container's Java trust store does not trust the certificate chain
presented for Maven Central. This often happens when a corporate proxy
replaces the site's certificate. Containers do not inherit Windows'
trusted certificates.

Obtain your organization's approved proxy CA certificate from your IT team,
or export the matching CA from Windows' Trusted Root Certification
Authorities store as **Base-64 encoded X.509 (.CER)**. Verify its fingerprint
with IT; use the CA certificate, not Maven Central's leaf certificate, and
never export a private key. Keep the certificate outside the project.

Supply that public certificate through a BuildKit secret:

```powershell
docker build --secret "id=maven_ca,src=C:\certificates\corporate-root-ca.cer" -t wildfly-junit-sample:1.0 .
```

The Dockerfile imports the optional certificate into the build-stage JDK's
trust store before running Maven. Invalid certificates fail the build.
TLS verification remains enabled, and the certificate/trust store is not
copied into the final WildFly image. Normal builds without this secret
continue to use the JDK's default trusted CAs. Docker Desktop uses BuildKit
by default.

After changing the certificate, add `--no-cache` to rebuild: changing secret
contents alone does not invalidate Docker's build cache.

This fixes Maven's in-container HTTPS trust. If pulling a base image fails
with a certificate error, Docker's own proxy/CA configuration must be fixed
separately. Runtime HTTPS calls from WildFly also need their own trust
configuration if they pass through the proxy.

## Deploy to an existing WildFly server

Start a Jakarta EE 10-compatible WildFly server with its local management
endpoint on port 9990, then run:

```powershell
.\mvnw.cmd clean package wildfly:deploy
```

The default connection uses local management authentication. A remote or
secured server requires the Maven plugin's management connection settings.
Do not store management passwords in this project.

To undeploy:

```powershell
.\mvnw.cmd wildfly:undeploy
```

## Windows certificate troubleshooting

If Maven reports `PKIX path building failed` on a corporate network, make sure
your organization's CA certificate is trusted. When it is already trusted in
the Windows certificate store, the JDK can use that store for the current
PowerShell session:

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
```

Retry the build in the same terminal. This keeps TLS certificate verification
enabled; do not disable verification to work around certificate errors.

## Project layout

```text
pom.xml
Dockerfile                     Multi-stage Maven build and WildFly runtime
.dockerignore                  Container build context exclusions
mvnw.cmd                       Maven wrapper for Windows
.mvn/wrapper                   Pinned Maven distribution
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
