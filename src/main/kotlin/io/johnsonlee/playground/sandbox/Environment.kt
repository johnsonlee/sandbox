package io.johnsonlee.playground.sandbox

import android.os._Original_Build
import com.android.SdkConstants
import com.android.ide.common.rendering.api.LayoutlibCallback
import com.android.ide.common.xml.AndroidManifestParser
import com.android.layoutlib.bridge.Bridge
import com.android.resources.aar.AarSourceResourceRepository
import com.android.resources.aar.FrameworkResourceRepository
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Objects
import io.johnsonlee.playground.sandbox.resources.AppResourceRepository
import io.johnsonlee.playground.util.getFieldReflectively
import io.johnsonlee.playground.util.setStaticValue
import io.johnsonlee.playground.util.toCanonicalFile
import okio.withLock
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

data class Environment(
    val appDir: File = System.getProperty("user.dir").toCanonicalFile(),
    val libDir: File = appDir.resolve("libs"),
    val resourcePackageNames: List<String> = getResourcePackageNames(libDir),
    val localResourceDirs: List<String> = emptyList(),
    val moduleResourceDirs: List<String> = emptyList(),
    val libraryResourceDirs: List<String> = getLibraryResourceDirs(libDir),
    val allModuleAssetDirs: List<String> = emptyList(),
    val libraryAssetDirs: List<String> = getLibraryAssetDirs(libDir),
    val androidHome: File = Environment.androidHome,
    val compileSdkVersion: Int = 31,
    val platformDir: File = androidHome.resolve("platforms").resolve("android-${compileSdkVersion}")
) {

    val resDir: File = appDir.resolve("res")

    val assetsDir: File = appDir.resolve("assets")

    @JsonIgnore
    val layoutlibCallback: LayoutlibCallback = LayoutlibCallbackImpl(this)

    val platformDataResDir = platformDir.resolve("data").resolve("res")

    @JsonIgnore
    val frameworkResources = FrameworkResourceRepository.create(platformDataResDir.toPath(), emptySet(), null, false)

    @JsonIgnore
    val projectResources = AppResourceRepository.create(
        localResourceDirectories = localResourceDirs.map(::File),
        moduleResourceDirectories = moduleResourceDirs.map(::File),
        libraryRepositories = libraryResourceDirs.map { dir ->
            val resourceDirPath = Paths.get(dir)
            AarSourceResourceRepository.create(resourceDirPath, resourceDirPath.parent.toFile().name)
        }
    )

    init {
        if (!platformDir.exists()) {
            val platformVersion = platformDir.name.substringAfterLast('-')
            throw FileNotFoundException("Missing platform version ${platformVersion}. Install with sdkmanager --install \"platforms;android-${platformVersion}\"")
        }
    }

    fun newBridge(): Bridge = run {
        val platformDataRoot = File(System.getProperty("user.dir"))
        val platformDataDir = platformDataRoot.resolve("data")
        val fontLocation = platformDataDir.resolve("fonts")
        val nativeLibLocation = platformDataDir.resolve(Environment.nativeLibDir)
        val icuLocation = platformDataDir.resolve("icu").resolve("icudt70l.dat")
        val keyboardLocation = platformDataDir.resolve("keyboards").resolve("Generic.kcm")
        val buildProp = platformDir.resolve("build.prop")
        val attrs = platformDataResDir.resolve("values").resolve("attrs.xml")
        val systemProperties = DeviceModel.loadProperties(buildProp) + mapOf("debug.choreographer.frametime" to "false")

        Bridge().apply {
            check(
                init(
                    systemProperties,
                    fontLocation,
                    nativeLibLocation.path,
                    icuLocation.path,
                    arrayOf(keyboardLocation.path),
                    DeviceModel.getEnumMap(attrs),
                    LayoutLogger
                )
            )

            configureBuildProperties()
            forcePlatformSdkVersion(compileSdkVersion)

            Bridge.getLock().withLock {
                Bridge.setLog(LayoutLogger)
            }
        }
    }

    override fun hashCode(): Int = Objects.hashCode(
        appDir,
        libDir,
        resourcePackageNames,
        localResourceDirs.map(String::toCanonicalFile),
        moduleResourceDirs.map(String::toCanonicalFile),
        libraryResourceDirs.map(String::toCanonicalFile),
        allModuleAssetDirs.map(String::toCanonicalFile),
        libraryAssetDirs.map(String::toCanonicalFile),
        androidHome,
        compileSdkVersion,
        platformDir,
        resDir,
        assetsDir
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Environment) return false
        return appDir == other.appDir
                && libDir == other.libDir
                && resourcePackageNames == other.resourcePackageNames
                && localResourceDirs.map(String::toCanonicalFile) == other.localResourceDirs.map(String::toCanonicalFile)
                && moduleResourceDirs.map(String::toCanonicalFile) == other.moduleResourceDirs.map(String::toCanonicalFile)
                && libraryResourceDirs.map(String::toCanonicalFile) == other.libraryResourceDirs.map(String::toCanonicalFile)
                && allModuleAssetDirs.map(String::toCanonicalFile) == other.allModuleAssetDirs.map(String::toCanonicalFile)
                && libraryAssetDirs.map(String::toCanonicalFile) == other.libraryAssetDirs.map(String::toCanonicalFile)
                && androidHome == other.androidHome
                && compileSdkVersion == other.compileSdkVersion
                && platformDir == other.platformDir
                && resDir == other.resDir
                && assetsDir == other.assetsDir
    }

    companion object {

        val androidHome: File
            get() = System.getenv("ANDROID_SDK_ROOT")?.toCanonicalFile()
                ?: System.getenv("ANDROID_HOME")?.toCanonicalFile()
                ?: androidSdkPath.toCanonicalFile()

        val nativeLibDir: String
            get() {
                val osName = System.getProperty("os.name").lowercase()
                val osLabel = when {
                    osName.startsWith("windows") -> "win"
                    osName.startsWith("mac") -> {
                        val arch = System.getProperty("os.arch").lowercase()
                        if (arch.startsWith("x86")) "mac" else "mac-arm"
                    }

                    else -> "linux"
                }
                return "${osLabel}/lib64"
            }

        private val androidSdkPath: String = run {
            val osName = System.getProperty("os.name").lowercase()
            if (osName.startsWith("windows")) {
                "${System.getProperty("user.home")}\\AppData\\Local\\Android\\Sdk"
            } else if (osName.startsWith("mac")) {
                "${System.getProperty("user.home")}/Library/Android/sdk"
            } else {
                "/usr/local/share/android-sdk"
            }
        }
    }
}

@JvmSynthetic
internal val logger = LoggerFactory.getLogger("sandbox")

private fun getLibraries(libDir: File): List<File> {
    return libDir.listFiles { f ->
        f.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML).exists()
    }?.toList() ?: emptyList()
}

private fun getResourcePackageNames(libDir: File): List<String> {
    return getLibraries(libDir).map {
        AndroidManifestParser.parse(it.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML).toPath()).`package`
    }
}

private fun getLibraryResourceDirs(libDir: File): List<String> {
    return getLibraries(libDir).map {
        it.resolve("res")
    }.filter(File::exists).map(File::getPath)
}

private fun getLibraryAssetDirs(libDir: File): List<String> {
    return getLibraries(libDir).map {
        it.resolve("assets")
    }.filter(File::exists).map(File::getPath)
}

private fun configureBuildProperties() {
    val buildClass = Class.forName("android.os.Build")
    val originalBuildClass = _Original_Build::class.java

    copyFieldsValue(originalBuildClass, buildClass)

    buildClass.classes.forEach { inner ->
        val originalInnerClass = originalBuildClass.classes.single { it.simpleName == inner.simpleName }
        copyFieldsValue(originalInnerClass, inner)
    }
}

private fun copyFieldsValue(from: Class<*>, to: Class<*>) {
    to.fields.forEach {
        try {
            val originalField = from.getField(it.name)
            to.getFieldReflectively(it.name).setStaticValue(originalField.get(null))
        } catch (e: Throwable) {
            logger.warn("Failed to set ${to.name}.${it.name}")
        }
    }
}

private fun forcePlatformSdkVersion(compileSdkVersion: Int) {
    val buildVersionClass = try {
        Class.forName("android.os.Build\$VERSION")
    } catch (e: ClassNotFoundException) {
        return
    }
    buildVersionClass.getFieldReflectively("SDK_INT").setStaticValue(compileSdkVersion)
}
