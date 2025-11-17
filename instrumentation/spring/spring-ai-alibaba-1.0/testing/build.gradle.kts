plugins {
  id("otel.java-conventions")
}
otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
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
  api(project(":testing-common"))
  api("com.alibaba.cloud.ai:spring-ai-alibaba-core:1.0.0.3")
  api(project(":instrumentation-api-incubator"))
}
