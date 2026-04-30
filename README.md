# iw-support

Shared support library for Iterative Works / e-BS projects, providing ZIO-based building blocks for
HTTP APIs, persistence, UI, and cross-cutting concerns.

## Using iw-support

### Mill

```scala
mvn"works.iterative.support::iw-support-core::0.1.14"
```

The `::` separator is Mill's cross-version notation — the Scala version suffix (`_3`) is appended
automatically. Use a single `:` only if you need a plain (non-cross-built) coordinate.

### sbt

```scala
"works.iterative.support" %% "iw-support-core" % "0.1.14"
```

### Resolver: e-BS Nexus

Add the e-BS Nexus resolver to your build if it is not already present. Internal e-BS projects
already have this configured.

**Mill** (`build.mill`):

```scala
def repositoriesTask = T.task {
  super.repositoriesTask() ++ Seq(
    coursier.maven.MavenRepository("https://nexus.e-bs.cz/repository/maven-releases/"),
    coursier.maven.MavenRepository("https://nexus.e-bs.cz/repository/maven-snapshots/")
  )
}
```

**sbt** (`build.sbt`):

```scala
resolvers ++= Seq(
  "e-BS Nexus Releases"  at "https://nexus.e-bs.cz/repository/maven-releases/",
  "e-BS Nexus Snapshots" at "https://nexus.e-bs.cz/repository/maven-snapshots/"
)
```

### Resolver: GitHub Packages

To resolve from GitHub Packages you need a GitHub Personal Access Token (PAT) with the
`read:packages` scope.

**Mill** (`build.mill`):

```scala
def repositoriesTask = T.task {
  super.repositoriesTask() ++ Seq(
    coursier.maven.MavenRepository("https://maven.pkg.github.com/iterative-works/support")
  )
}
```

**sbt** (`build.sbt`):

```scala
resolvers += "GitHub Packages iw-support" at "https://maven.pkg.github.com/iterative-works/support"
credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "<github-user>",
  "<pat>"
)
```

**Coursier credentials** (`~/.config/coursier/credentials.properties`):

```
host=maven.pkg.github.com
username=<github-user>
password=<pat>
```

Mill (via Coursier) picks these up automatically; no extra resolver credential wiring needed in
`build.mill`.

### Available artifacts

| Artifact | Description |
|----------|-------------|
| `iw-support-core` | Core ZIO building blocks |
| `iw-support-tapir` | Tapir HTTP API support |
| `iw-support-mongo` | MongoDB persistence |
| `iw-support-sqldb` | SQL database support |
| `iw-support-server-http` | HTTP server utilities |
| `iw-support-all` | Aggregate — all modules |

Run `./mill resolve __.publishArtifacts` for the full list of published coordinates.

## UI scenarios development

Pro vývoj je potřeba provést tyto kroky:
- `./mill -w scenariosUI.fastLinkJS`
- `cd ui/scenarios/`
- `yarn` pokud není nainstalován vite
- `yarn vite .`
