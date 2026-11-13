rootProject.name = "hopsync"

// core: domain, sync engine and use cases (no Swing, no HTTP client)
// platform: adapters for HTTP, settings and the file system
// desktop: Swing app, composition root and the distributable JAR
include("core", "platform", "desktop")
