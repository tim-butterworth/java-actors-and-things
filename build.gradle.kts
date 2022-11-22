plugins {
    id("java")
}

tasks.withType(JavaCompile::class.java) {
    version = JavaVersion.VERSION_1_8
}
group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation(name = "rxjava", group = "io.reactivex.rxjava3", version = "3.1.5")
    implementation("com.typesafe.akka:akka-actor-typed_3:2.6.20")

    implementation("io.projectreactor:reactor-core:3.4.24")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}