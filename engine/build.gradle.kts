plugins {
    application
}

description = "3D-клиент на jMonkeyEngine: сцена фермы, робот, камера, освещение."

dependencies {
    implementation(project(":sandbox"))

    implementation(libs.jme.core)
    implementation(libs.jme.desktop)
    implementation(libs.jme.effects) // пост-обработка: SSAO, FXAA, bloom
    // LWJGL3-бэкенд + нативные библиотеки; напрямую используется для буфера обмена (GLFW).
    implementation(libs.jme.lwjgl3)
}

application {
    mainClass.set("com.farmgame.engine.Main")
    // GLFW на macOS требует запуска в главном потоке.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
