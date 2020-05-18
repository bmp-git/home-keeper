group = "org.bmp"
version = "1.0"

val masPath = "homeAgents.mas2j"

plugins {
    java
}

task<JavaExec>("run") {
    group = "run"
    classpath = sourceSets.getByName("main").runtimeClasspath
    main = "jason.infra.centralised.RunCentralisedMAS"
    args(masPath)
    standardInput = System.`in`
}

repositories {
    mavenCentral()
    maven("http://jacamo.sourceforge.net/maven2")
    maven("https://jade.tilab.com/maven")
}

sourceSets {
    main {
        resources {
            srcDir("src/main/asl")
        }
    }
}

dependencies {
    implementation("org.jason-lang", "jason", "2.5-SNAPSHOT")
    testImplementation("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}