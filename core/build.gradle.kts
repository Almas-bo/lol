plugins {
    `java-library`
}

description = "Логика фермы: грядки, культуры, ресурсы, сущности. Без зависимостей от графики."

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
