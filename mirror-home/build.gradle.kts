group = "org.bmp"
version = "0.0.0"

plugins {
    scala
    application
    //id("org.scoverage") version "3.0.0"
    id("com.github.maiflai.scalatest") version "0.25"
}

application {
    mainClassName = "webserver.WebServer"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    //Default
    implementation("org.scala-lang:scala-library:2.12.11")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.typesafe.scala-logging:scala-logging_2.12:3.9.2")

    //Evaluating scala file at runtime
    implementation("org.scala-lang:scala-compiler:2.12.11")

    //Akka: http server, json, mqtt client
    implementation("com.typesafe.akka:akka-http_2.12:10.1.11")
    implementation("com.typesafe.akka:akka-stream_2.12:2.5.26")
    implementation("com.typesafe.akka:akka-http-spray-json_2.12:10.1.11")
    implementation("com.lightbend.akka:akka-stream-alpakka-mqtt_2.12:2.0.0-RC1")

    //Sourcecode
    implementation("com.lihaoyi:sourcecode_2.12:0.2.1")

    //JWT authentication
    implementation("com.pauldijou:jwt-core_2.12:4.3.0")

    //spire.math ULong, UByte
    implementation("org.spire-math:spire_2.12:0.13.0")

    //JavaCV
    implementation("org.bytedeco:javacv-platform:1.5.2")

    //Tests
    testImplementation("org.scalatest:scalatest_2.12:3.0.1")
    testImplementation("com.typesafe.akka:akka-stream-testkit_2.12:2.5.26")
    testImplementation("com.typesafe.akka:akka-http-testkit_2.12:10.1.11")

    testRuntimeOnly("org.pegdown:pegdown:1.4.2")
    //testImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
    //scoverage("org.scoverage:scalac-scoverage-plugin_2.12:1.3.1")
    //scoverage("org.scoverage:scalac-scoverage-runtime_2.12:1.3.1")
}