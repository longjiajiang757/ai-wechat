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

    // 前端只是把网页版界面挂进 APK 的附赠功能，这里构建失败不该让整个 APK 构建失败
    // （例如本机没装 pnpm）。输出目录在下面的配置阶段就建好，保证 Gradle 输出声明成立。
    //
    // 注意：这里不能再加 doLast 之类的执行期闭包。闭包会捕获脚本级对象
    // （webStaticResourcesDir、logger 等），而 Gradle 9 的配置缓存无法序列化
    // 脚本对象引用，会在构建收尾时判整个构建失败（APK 明明已经打出来了）。
    isIgnoreExitValue = true
}

// 提前建目录：前端没构建成功时，输出目录也真实存在，
// 下游 :app 打包资源和 Gradle 的输出声明都不会出错。
webStaticResourcesDir.asFile.mkdirs()

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
