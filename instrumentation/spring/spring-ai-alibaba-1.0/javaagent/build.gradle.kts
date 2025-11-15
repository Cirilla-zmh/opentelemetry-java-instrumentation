plugins {
    id("otel.javaagent-instrumentation")
}
otelJava {
    // Spring AI Alibaba requires java 17 (same as Spring AI)
    minJavaVersionSupported.set(JavaVersion.VERSION_17)
}
muzzle {
    pass {
        group.set("com.alibaba.cloud.ai")
        module.set("spring-ai-alibaba-core")
        versions.set("(,)")
    }
}
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.spring.io/milestone")
        content {
            includeGroup("org.springframework.ai")
            includeGroup("org.springframework.boot")
            includeGroup("org.springframework")
        }
    }
    maven {
        url = uri("https://repo.spring.io/snapshot")
        content {
            includeGroup("org.springframework.ai")
            includeGroup("org.springframework.boot")
            includeGroup("org.springframework")
        }
        mavenContent {
            snapshotsOnly()
        }
    }
}
dependencies {
    library("io.projectreactor:reactor-core:3.7.0")
    library("com.alibaba.cloud.ai:spring-ai-alibaba-core:1.0.0.3")
    implementation(project(":instrumentation:reactor:reactor-3.1:library"))
    bootstrap(project(":instrumentation:reactor:reactor-3.1:bootstrap"))
    testImplementation(project(":instrumentation:spring:spring-ai-alibaba-1.0:testing"))
}
tasks {
    withType<Test>().configureEach {
        val latestDepTest = findProperty("testLatestDeps") as Boolean
        systemProperty("testLatestDeps", latestDepTest)
        // spring ai requires java 17
        if (latestDepTest) {
            otelJava {
                minJavaVersionSupported.set(JavaVersion.VERSION_17)
            }
        }
        // TODO run tests both with and without genai message capture
        systemProperty("otel.instrumentation.genai.capture-message-content", "true")
        systemProperty("collectMetadata", findProperty("collectMetadata")?.toString() ?: "false")
    }
}