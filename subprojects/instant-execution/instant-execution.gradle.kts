import build.futureKotlin
import org.gradle.gradlebuild.unittestandcompile.ModuleType

plugins {
    `kotlin-library`
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":messaging"))
    implementation(project(":logging"))
    implementation(project(":coreApi"))
    implementation(project(":core"))
    implementation(project(":modelCore"))
    implementation(project(":files"))

    implementation(library("groovy"))
    implementation(library("slf4j_api"))
    implementation(futureKotlin("stdlib-jdk8"))

    integTestImplementation(project(":toolingApi"))

    integTestImplementation(library("guava"))
    integTestImplementation(library("ant"))
    integTestImplementation(library("inject"))

    integTestRuntimeOnly(project(":toolingApiBuilders"))
}

gradlebuildJava {
    moduleType = ModuleType.CORE
}

testFixtures {
    from(":core")
}
