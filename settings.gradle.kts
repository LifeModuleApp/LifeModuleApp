pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LifeModule"
include(":app")
include(":core")
include(":features:gym")
include(":features:nutrition")
include(":features:health")
include(":features:planner")
include(":features:shopping")
include(":features:analytics")
include(":features:logbook")
include(":features:scanner")
include(":features:recipes")
