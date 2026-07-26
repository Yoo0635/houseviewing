import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun loadEnvFile(path: String): Map<String, String> {
    // Gradle project.file() 는 상대 경로 시 :app 모듈 기준이라, 절대 경로는 File 로 직접 연다
    val envFile = File(path)
    if (!envFile.exists() || !envFile.isFile) return emptyMap()
    // UTF-8 BOM 이 있으면 첫 키가 깨져 KAKAO_REST_API_KEY 를 못 읽는 경우가 있음
    val text = envFile.readText().removePrefix("\uFEFF")
    return text.lineSequence()
        .map { line ->
            var s = line.trim()
            if (s.lowercase().startsWith("export ")) s = s.substring(7).trim()
            s
        }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val idx = line.indexOf("=")
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim().removeSurrounding("\"")
            key to value
        }
}

fun normalizeSecret(raw: String): String {
    return raw.trim()
        .removeSurrounding("'")
        .removeSurrounding("\"")
}

data class KakaoKeyResolve(val key: String, val sourceLabel: String?)

fun resolveKakaoKey(): KakaoKeyResolve {
    // settings.gradle 이 있는 폴더 = capstoneNew/app, 저장소 루트 .env = 그 부모(capstoneNew)/.env
    val root = rootProject.projectDir
    val envCandidates = listOfNotNull(
        root.parentFile?.resolve(".env"), // CAPSTONENEW/.env (탐색기에 보이는 그 파일)
        root.resolve(".env"),
        root.resolve("../.env").normalize(),
        root.resolve("../../.env").normalize()
    ).distinct()

    for (candidate in envCandidates) {
        val envMap = loadEnvFile(candidate.absolutePath)
        val value = normalizeSecret(envMap["KAKAO_REST_API_KEY"].orEmpty())
        if (value.isNotBlank()) {
            return KakaoKeyResolve(value, candidate.absolutePath)
        }
    }

    // .env 를 못 찾을 때(경로/동기화) — Android 표준 local.properties 폴백
    val localProps = root.resolve("local.properties")
    if (localProps.exists()) {
        runCatching {
            Properties().apply { localProps.inputStream().use { load(it) } }
                .getProperty("KAKAO_REST_API_KEY")
                ?.let { normalizeSecret(it) }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()?.let {
            return KakaoKeyResolve(it, localProps.absolutePath)
        }
    }

    return KakaoKeyResolve("", null)
}

val kakaoResolve = resolveKakaoKey()
val kakaoRestApiKey = kakaoResolve.key.also { key ->
    val src = kakaoResolve.sourceLabel
    if (key.isBlank()) {
        logger.lifecycle(
            "[houseViewingApp] KAKAO_REST_API_KEY 가 비어 있습니다. " +
                "repo 루트 .env 에 KAKAO_REST_API_KEY= 한 줄 추가 또는 app/local.properties 동일 키, " +
                "그 다음 Gradle Sync. (Docker spring-server 환경과는 별개입니다.)"
        )
    } else {
        logger.lifecycle(
            "[houseViewingApp] KAKAO_REST_API_KEY 로드됨 (길이 ${key.length}, 출처: $src)"
        )
    }
}

android {
    namespace = "com.capstone.houseviewingapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.capstone.houseviewingapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 에뮬레이터: http://10.0.2.2:8080/ — PC 의 localhost:8080 (Docker Spring 등)
        // 실제 기기(USB): PC LAN IP 또는 `adb reverse tcp:8080 tcp:8080` + http://127.0.0.1:8080/
        buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080/\"")
        buildConfigField(
            "String",
            "KAKAO_REST_API_KEY",
            "\"${kakaoRestApiKey.replace("\"", "\\\"")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation(libs.androidx.room.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}