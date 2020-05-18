group = "org.bmp"
version = "1.0"

val masPath = "homeAgents.mas2j"

plugins {
    scala
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
    jcenter()
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
    implementation("org.scala-lang:scala-library:2.12.11")
    //JSON
    implementation("org.jason-lang", "jason", "2.5-SNAPSHOT")

    //JSON
    implementation("com.typesafe.play", "play-json_2.12", "2.8.1")

    //HTTP client
    implementation("com.softwaremill.sttp.client","core_2.12", "2.1.1")
}