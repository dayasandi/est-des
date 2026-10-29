plugins {
    alias(libs.plugins.spring.boot) apply false
}

subprojects {
    group = "br.com.garage"

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release.set(21)
            options.encoding = "UTF-8"

            options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing", "-Werror"))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
