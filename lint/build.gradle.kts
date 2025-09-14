/*plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
*/

/*kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}*/

plugins {
    `java-library`
//    kotlin("jvm") version "1.9.23"
    id("maven-publish") // optional, if you want to publish the jar
    id("com.android.lint")
    kotlin("jvm")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Match these to your AGP
    val lintVersion = "31.4.1"    // for AGP 8.4.x
    val junitVersion = "4.13.2"

    compileOnly("com.android.tools.lint:lint-api:$lintVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation("com.android.tools.lint:lint-tests:$lintVersion")
    testImplementation("junit:junit:$junitVersion")
}

// optional publishing (local/remote)
publishing {
    publications {
        create<MavenPublication>("lint") {
            from(components["java"])
            groupId = "com.yourorg.tools"
            artifactId = "vipin-lint"
            version = "0.1.0"
        }
    }
}
