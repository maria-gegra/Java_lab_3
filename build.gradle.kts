plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "ru.university"
version = "1.2"

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.graphics")
}

application { mainClass.set("ru.university.socialnetwork.Main") }

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
    testRuntimeOnly("org.testfx:openjfx-monocle:21.0.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

sourceSets {
    create("bench") {
        compileClasspath += sourceSets["main"].output + configurations.runtimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<JavaExec>("serializationBenchmark") {
    group = "verification"
    description = "Сравнение JSON и бинарной сериализации на 10000 записей (среднее по 5 прогонам)"
    classpath = sourceSets["bench"].runtimeClasspath
    mainClass.set("ru.university.socialnetwork.bench.SerializationBenchmark")
    jvmArgs("-Xms256m", "-Xmx768m", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Замеры операций над собственным графом и аналогом из java.util"
    classpath = sourceSets["bench"].runtimeClasspath
    mainClass.set("ru.university.socialnetwork.bench.Benchmark")
    jvmArgs("-Xms512m", "-Xmx1g", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("glass.platform", "Monocle")
    systemProperty("monocle.platform", "Headless")
    systemProperty("prism.order", "sw")
    systemProperty("headless.geometry", "1600x900-32")
    systemProperty("java.awt.headless", "true")
    systemProperty("file.encoding", "UTF-8")
    testLogging { events("passed", "skipped", "failed") }
}
