group = "org.bmp"
version = "1.0.0"

plugins {
    scala
    application
    id("org.scoverage") version "4.0.1"
    id("com.github.maiflai.scalatest") version "0.24"
}

application {
    mainClassName = "Main"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.1")
    //implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.typesafe.scala-logging:scala-logging_2.13:3.9.2")

    //compile("org.scala-lang:scala-library:2.13.1")

    testImplementation("org.scalatest:scalatest_2.13:3.0.8")
    testImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    testRuntimeOnly("org.pegdown:pegdown:1.4.2")
    scoverage("org.scoverage:scalac-scoverage-plugin_2.13:1.4.0")
    scoverage("org.scoverage:scalac-scoverage-runtime_2.13:1.4.0")
}