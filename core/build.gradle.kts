plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {

    testFixturesApi(platform(libs.spring.boot.dependencies))
    testFixturesApi("org.junit.jupiter:junit-jupiter-api")
    testFixturesApi("org.assertj:assertj-core")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
