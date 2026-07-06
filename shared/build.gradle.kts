import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import java.net.URI
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.kotlinSerialization)
}

// Read secrets from local.properties (gitignored). Fall back to empty strings
// so a fresh clone still compiles — runtime code will show a clear error if
// SUPABASE_URL / SUPABASE_ANON_KEY are needed and unset.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}
fun localProp(name: String, fallback: String = ""): String =
    localProperties.getProperty(name) ?: fallback

kotlin {
    jvmToolchain(17)

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
       namespace = "org.moashraf.sayva.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.sqldelight.driver.android)
            // Firebase BOM pins native SDK versions transitively used by GitLive
            // (firebase-common-ktx, firebase-auth-ktx, firebase-analytics, firebase-crashlytics-ktx).
            // GitLive does NOT pin these itself — without the BOM, resolution fails with
            // "Could not find com.google.firebase:...:." (empty version).
            implementation(project.dependencies.platform(libs.firebase.bom))
            // EncryptedSharedPreferences backing for SecureStorage on Android.
            implementation(libs.androidx.security.crypto)
            // BiometricPrompt (FragmentActivity-based) for BiometricPrompt gateway.
            implementation(libs.androidx.biometric)
            // FragmentActivity for MainActivity — required by androidx.biometric.
            implementation(libs.androidx.fragment.ktx)
            // rememberLauncherForActivityResult — used by PermissionRequester.
            implementation(libs.androidx.activity.compose)
            // TFLite runtime for on-device sign recognition (Track A smoke test,
            // Track B real fingerspelling model). iOS uses CoreML instead (see
            // iosMain/ml/adapters/TfliteRuntimeAdapter.ios.kt — currently stubbed).
            implementation(libs.tensorflow.lite)

            // CameraX — Preview + ImageAnalysis + PreviewView for the Phase 2
            // camera controller. Compose interop via AndroidView.
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)

            // MediaPipe Hand Landmarker for 21-landmark detection per frame.
            // iOS counterpart is deferred to a Mac session (CocoaPods / SPM).
            implementation(libs.mediapipe.tasks.vision)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings.noArg)
            implementation(libs.gitlive.firebase.common)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
            // Supabase-kt (auth only for now; add postgrest-kt later for DB)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            // Ktor engine required by supabase-kt at runtime on Android.
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
            // Ktor engine required by supabase-kt at runtime on iOS.
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            // Test-only: TestScope, runTest, UnconfinedTestDispatcher for
            // deterministic testing of pipeline coroutines that internally
            // use Dispatchers.Default / Dispatchers.Main.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

sqldelight {
    databases {
        create("SayvaDatabase") {
            packageName.set("org.moashraf.sayva.db")
        }
    }
}

// Language Pack distribution — every pack in ml/packs/ is turned into a
// bundled asset tree under composeResources/files/language_packs/{code}/.
// The Python script (`ml/scripts/generate_pack.py`) validates the manifest,
// copies model files with simple stable names, and emits `manifest.json`
// with vocabularies + labels inlined. Same pattern as SQLDelight codegen:
// the produced artifacts are git-committed; the Gradle task fails the build
// if they've drifted from what the current pack sources would produce.
//
// `verifyPacks` runs on `check` — CI catches stale distributed artifacts.
// Local developers regenerate with `./gradlew generatePacks`. Naming
// follows docs/PACKS_WORKFLOW.md, which is authoritative for the pack
// dev workflow across all phases (Phase 1 today: explicit task; Phase 2:
// verify-on-assemble; Phase 3: split CI + OTA publish).
val mlDir = rootProject.file("ml")
val packScript = mlDir.resolve("scripts/generate_pack.py")
val packsSourceDir = mlDir.resolve("packs")
val packsResourceDir = layout.projectDirectory
    .dir("src/commonMain/composeResources/files/language_packs")

val verifyPacks = tasks.register<Exec>("verifyPacks") {
    group = "verification"
    description =
        "Fails if ml/packs/*/ has been edited without regenerating the distributed manifest.json + model files."
    workingDir = mlDir
    commandLine("uv", "run", "python", packScript.absolutePath, "--check")
    inputs.dir(packsSourceDir).withPropertyName("packs")
    inputs.file(packScript).withPropertyName("script")
    inputs.dir(packsResourceDir).withPropertyName("distributedPacks")
    outputs.upToDateWhen { true }
    isIgnoreExitValue = false
}

val generatePacks = tasks.register<Exec>("generatePacks") {
    group = "build"
    description =
        "Distributes every ml/packs/*/ into composeResources/files/language_packs/."
    workingDir = mlDir
    commandLine("uv", "run", "python", packScript.absolutePath)
    inputs.dir(packsSourceDir)
    inputs.file(packScript)
    outputs.dir(packsResourceDir)
}

tasks.named("check").configure { dependsOn(verifyPacks) }

// ---------------------------------------------------------------------------
// MediaPipe HandLandmarker model — auto-fetch during Android build
// ---------------------------------------------------------------------------
//
// The recognition pipeline's HandDetector on Android loads
// `hand_landmarker.task` from `src/androidMain/assets/mediapipe/`. The file
// is a ~7 MB binary maintained by Google (and rotated occasionally as they
// re-train the landmark model), so we do NOT commit it to git — every
// developer / CI runner fetches it once via this task.
//
// The task downloads only when the target file is absent OR its SHA-256
// doesn't match the pinned value below. Otherwise it's a no-op. That keeps
// clean builds fast (~0ms verify) and offline-safe.
//
// Wired as an input to `mergeAndroidMainAssets` so `assembleDebug`/`assemble`
// pull it automatically on a fresh clone. No `curl` step in the README, no
// manual CI setup — checkout, build, run.
val handLandmarkerUrl = uri(
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
)
// SHA-256 of the pinned model version. If the download's hash differs we
// refuse to write it — protects against a compromised CDN swapping bytes.
// Set to empty string to skip verification during development if Google
// rotates the file and we need to re-pin.
// Pinned SHA-256 of the MediaPipe hand_landmarker.task binary Google served at
// the URL above. Verified on the first successful download on 2026-07-06. If
// Google republishes the file the download task will refuse to write the new
// bytes; re-pin here after confirming the new hash against a trusted source.
val handLandmarkerSha256 = "fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"
val handLandmarkerAsset = layout.projectDirectory
    .file("src/androidMain/assets/mediapipe/hand_landmarker.task")

val downloadHandLandmarkerModel = tasks.register("downloadHandLandmarkerModel") {
    group = "build setup"
    description = "Downloads MediaPipe HandLandmarker model into androidMain/assets if missing."

    // Capture as locals so the configuration cache can serialize the task
    // action without dragging in Gradle-script object references.
    val destFile = handLandmarkerAsset.asFile
    val expectedSha = handLandmarkerSha256
    val sourceUrlString = handLandmarkerUrl.toString()
    val rootDirValue = rootDir

    outputs.file(destFile)
    outputs.upToDateWhen {
        if (!destFile.exists()) return@upToDateWhen false
        if (expectedSha.isEmpty()) return@upToDateWhen true
        HandLandmarkerDigest.sha256(destFile).equals(expectedSha, ignoreCase = true)
    }

    doLast {
        destFile.parentFile.mkdirs()
        val url = URI(sourceUrlString).toURL()
        val tempFile = destFile.resolveSibling(destFile.name + ".part")
        println("Downloading MediaPipe HandLandmarker model from $url")
        url.openStream().use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        val actualSha = HandLandmarkerDigest.sha256(tempFile)
        if (expectedSha.isNotEmpty() && !actualSha.equals(expectedSha, ignoreCase = true)) {
            tempFile.delete()
            throw GradleException(
                "MediaPipe HandLandmarker checksum mismatch. Expected $expectedSha, got $actualSha. " +
                    "If Google published a new model, verify manually and update `handLandmarkerSha256` in shared/build.gradle.kts."
            )
        }
        if (destFile.exists()) destFile.delete()
        tempFile.renameTo(destFile)
        println(
            "Wrote ${destFile.relativeTo(rootDirValue)} (${destFile.length() / 1024} KB, sha256=$actualSha)"
        )
    }
}

// Hook the download into every Android asset-merging path so `assembleDebug`,
// `assembleRelease`, and instrumented-test builds all get the asset without
// developers thinking about it. Wildcard match survives AGP renames.
tasks.matching { it.name.startsWith("merge") && it.name.contains("Assets") }.configureEach {
    dependsOn(downloadHandLandmarkerModel)
}

// Compute lowercase-hex SHA-256 of a file. Lives on an `object` (not a script
// top-level fun) so `downloadHandLandmarkerModel`'s captured references stay
// serializable under the Gradle configuration cache — top-level script fun's
// carry a Project reference which the cache refuses to store.
object HandLandmarkerDigest {
    fun sha256(file: java.io.File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}

// Compile-time constants surfaced as `org.moashraf.sayva.buildkonfig.BuildKonfig`.
// Values come from local.properties (gitignored) so keys never land in the tracked
// codebase. See local.properties.template for the expected keys.
buildkonfig {
    packageName = "org.moashraf.sayva.buildkonfig"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "BACKEND",
            localProp("sayva.backend", fallback = "firebase"),
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_URL",
            localProp("sayva.supabase.url"),
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_ANON_KEY",
            localProp("sayva.supabase.anonKey"),
        )
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Force every JVM test task (including AGP's AndroidUnitTest, which extends Test)
// to use a Java 17 launcher. Without this, AGP's own default falls back to
// Java 11 for testAndroidHostTest — which Gradle then tries to auto-provision
// via toolchain and fails on machines without JDK 11 installed.
// The Foojay resolver in settings.gradle.kts is the download fallback if the
// requested launcher isn't locally installed.
val javaToolchains = extensions.getByType<JavaToolchainService>()
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    )
}