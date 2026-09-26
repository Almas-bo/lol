plugins {
    `java-library`
}

description = "Песочница: API для кода игрока, компиляция (javax.tools) и выполнение программ."

dependencies {
    // Crop и CropType входят в публичный API игрока, поэтому core экспортируется как api.
    api(project(":core"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
