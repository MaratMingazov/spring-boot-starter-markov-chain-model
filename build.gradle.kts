import org.gradle.kotlin.dsl.from


plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion // kotlin compiler
    kotlin("plugin.spring") version kotlinVersion // open classes for @Service, @Configuration annotations
    id("org.springframework.boot") version "3.4.4" // add Spring Boot Gradle Plugin (bootRun, bootJar tasks)
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish` // to publish to local artifactory
}

dependencies {
    // Spring Boot classes are needed only for compilation of annotations and BeanPostProcessors.
    // Consumers of the starter will provide Spring Boot at runtime,
    // so we keep it as compileOnly to avoid leaking Spring Boot into their dependency tree.
    compileOnly("org.springframework.boot:spring-boot-starter:3.5.4")

    implementation("tools.jackson.module:jackson-module-kotlin:3.0.+")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}


// Since it's not an application, we should disable bootJar. Otherwise gradle is looking for main class
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

// we would like to compile a library as a jar file
tasks.getByName<Jar>("jar") {
    enabled = true
}

// to be able to push into maven local
group = "com.github.maratmingazov"
version = "1.0.0"

java {
    withSourcesJar()
    withJavadocJar()
}

// ./gradlew clean build --refresh-dependencies
// ./gradlew build
// ./gradlew publishToMavenLocal
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}