plugins {
    id("java")
    application
}

group = "com.instana.dc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("io.opentelemetry:opentelemetry-api:1.28.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.28.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.28.0")
    implementation("io.opentelemetry:opentelemetry-semconv:1.28.0-alpha")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0-rc1")

    implementation("mysql:mysql-connector-java:5.1.46")
    implementation(files("DmJdbcDriver18.jar"))
    implementation(files("otel-sensorsdk-database-1.0.2.jar"))
}

application {
    mainClass.set("com.instana.dc.rdb.DataCollector")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
