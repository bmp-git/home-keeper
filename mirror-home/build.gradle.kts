group = "org.bmp"
version = "0.0.0"

plugins {
    scala
    application
    //id("org.scoverage") version "3.0.0"
    id("com.github.maiflai.scalatest") version "0.25"
}

application {
    mainClassName = "Main"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.12.11")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.typesafe.scala-logging:scala-logging_2.12:3.9.2")

    implementation("com.typesafe.akka:akka-http_2.12:10.1.11")
    implementation("com.typesafe.akka:akka-stream_2.12:2.5.26")

    implementation("org.scala-lang:scala-compiler:2.12.11")
    //compile("org.scala-lang:scala-library:2.13.1")

    testImplementation("org.scalatest:scalatest_2.12:3.0.1")
    testImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    testRuntimeOnly("org.pegdown:pegdown:1.4.2")
    //scoverage("org.scoverage:scalac-scoverage-plugin_2.12:1.3.1")
    //scoverage("org.scoverage:scalac-scoverage-runtime_2.12:1.3.1")
}