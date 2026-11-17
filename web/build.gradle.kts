import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("rikkahub.android.library")
}

val webUiDir = rootProject.layout.projectDirectory.dir("web-ui")
val webStaticResourcesDir = layout.projectDirectory.dir("src/main/resources/static")

val buildWebUi = tasks.register<Exec>("buildWebUi") {
    group = "build"
    description = "Build web-ui and copy its static output into the web module resources."

    workingDir = webUiDir.asFile

    // 先装依赖再构建：CI 上没有现成的 node_modules，只跑 build 会直接失败。
    // --frozen-lockfile 保证按 pnpm-lock.yaml 安装，避免版本漂移。
    val buildCommand = "pnpm install --frozen-lockfile && pnpm run build"
    when {
        Os.isFamily(Os.FAMILY_MAC) -> commandLine("zsh", "-ic", buildCommand)
        Os.isFamily(Os.FAMILY_WINDOWS) -> commandLine("cmd", "/c", buildCommand)
        else -> commandLine("sh", "-c", buildCommand)
    }

    inputs.files(
        webUiDir.file("package.json"),
        webUiDir.file("pnpm-lock.yaml"),
        webUiDir.file("components.json"),
        webUiDir.file("copy.ts"),
        webUiDir.file("react-router.config.ts"),
        webUiDir.file("tsconfig.json"),
        webUiDir.file("vite.config.ts"),
        webUiDir.file("vite-env.d.ts")
    )
    inputs.dir(webUiDir.dir("app"))
    inputs.dir(webUiDir.dir("public"))
    outputs.dir(webStaticResourcesDir)

    // 前端只是把网页版界面挂进 APK 的附赠功能。这里构建失败不该让整个 APK 构建失败
    // （例如本机没装 pnpm），下面 doLast 会补一个空目录，保证 Gradle 输出声明成立。
    isIgnoreExitValue = true

    doLast {
        // Gradle 9 移除了 Exec 任务的 exitValue 属性，这里改为直接看产物是否生成，
        // 不依赖已被删除的 API，跨 Gradle 版本都成立。
        val outputDir = webStaticResourcesDir.asFile
        val built = outputDir.exists() && outputDir.listFiles()?.isNotEmpty() == true
        if (!built) {
            logger.warn(
                "警告: web-ui 前端构建未成功，本次 APK 将不包含网页版界面资源。" +
                    "如需该功能，请先在本机安装 Node 22 + pnpm 11。"
            )
            outputDir.mkdirs()
        }
    }
}

android {
    namespace = "me.rerere.rikkahub.web"

    defaultConfig {
        minSdk = 24
    }
}

tasks.named("preBuild") {
    dependsOn(buildWebUi)
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // ktor server
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.conditional.headers)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.cors)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.sse)
    api(libs.ktor.server.cio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
