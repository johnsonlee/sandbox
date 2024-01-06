package io.johnsonlee.playground.sandbox

import com.android.SdkConstants
import com.android.ide.common.rendering.api.AssetRepository
import com.android.ide.common.rendering.api.LayoutlibCallback
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.ide.common.rendering.api.SessionParams
import com.android.ide.common.resources.ResourceRepository
import com.android.ide.common.resources.ResourceResolver
import com.android.ide.common.resources.getConfiguredResources
import com.android.layoutlib.bridge.Bridge
import com.android.layoutlib.bridge.android.RenderParamsFlags
import com.android.resources.LayoutDirection
import com.android.resources.ResourceType
import io.johnsonlee.playground.sandbox.parsers.LayoutPullParser

data class SessionParamsBuilder(
    val layoutlibCallback: LayoutlibCallback,
    val frameworkResources: ResourceRepository,
    val assetRepository: AssetRepository,
    val projectResources: ResourceRepository,
    val deviceModel: DeviceModel = DeviceModel.PIXEL_5,
    val renderingMode: SessionParams.RenderingMode = SessionParams.RenderingMode.SHRINK,
    val targetSdkVersion: Int = 21,
    val flags: Map<SessionParams.Key<*>, Any> = DEFAULT_FLAGS,
    val themeName: String? = DEFAULT_THEME,
    val isProjectTheme: Boolean = false,
    val layoutPullParser: LayoutPullParser = LayoutPullParser.createFromString(XML_FRAMELAYOUT),
    val projectKey: Any? = null,
    val minSdkVersion: Int = 0,
    val decor: Boolean = false,
    val supportsRtl: Boolean = true
) {

    fun withTheme(
        themeName: String,
        isProjectTheme: Boolean = false
    ) = copy(themeName = themeName, isProjectTheme = isProjectTheme)

    fun withTheme(themeName: String) = when {
        themeName.startsWith(SdkConstants.PREFIX_ANDROID) -> {
            withTheme(themeName.substring(SdkConstants.PREFIX_ANDROID.length), false)
        }

        else -> withTheme(themeName, true)
    }

    fun plusFlag(flag: SessionParams.Key<*>, value: Any) = copy(flags = flags + (flag to value))

    fun build(): SessionParams {
        require(themeName != null)

        val folderConfiguration = deviceModel.resourceConfiguration
        val platformResources = listOf(ResourceNamespace.ANDROID).associateWith {
            frameworkResources.getConfiguredResources(folderConfiguration).row(it)
        }
        val applicationResources = projectResources.getConfiguredResources(folderConfiguration).rowMap()
        val resourceResolver = ResourceResolver.create(
            platformResources + applicationResources,
            ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.STYLE, themeName),
        )
        val sessionParams = SessionParams(
            layoutPullParser,
            renderingMode,
            projectKey,
            deviceModel.hardwareConfig,
            resourceResolver,
            layoutlibCallback,
            minSdkVersion,
            targetSdkVersion,
            LayoutLogger
        )

        sessionParams.fontScale = deviceModel.fontScale
        sessionParams.uiMode = deviceModel.uiModeMask

        val localeQualifier = folderConfiguration.localeQualifier
        val layoutDirectionQualifier = folderConfiguration.layoutDirectionQualifier

        if (LayoutDirection.RTL == layoutDirectionQualifier.value && !Bridge.isLocaleRtl(localeQualifier.tag)) {
            sessionParams.locale = "ur"
        } else {
            sessionParams.locale = localeQualifier.tag
        }

        sessionParams.setRtlSupport(supportsRtl)

        flags.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            sessionParams.setFlag(key as SessionParams.Key<Any>, value)
        }
        sessionParams.setAssetRepository(assetRepository)
        if (!decor) {
            sessionParams.setForceNoDecor()
        }

        return sessionParams
    }

    companion object {

        private val DEFAULT_FLAGS: Map<SessionParams.Key<*>, Any> = mapOf(
            RenderParamsFlags.FLAG_DO_NOT_RENDER_ON_CREATE to true,
        )

        private val XML_FRAMELAYOUT = """
                |<?xml version="1.0" encoding="utf-8"?>
                |<FrameLayout
                |    xmlns:android="http://schemas.android.com/apk/res/android"
                |    android:layout_width="match_parent"
                |    android:layout_height="match_parent"
                |/>
                """.trimMargin()

        fun from(environment: Environment): SessionParamsBuilder = SessionParamsBuilder(
            layoutlibCallback = environment.layoutlibCallback,
            frameworkResources = environment.frameworkResources,
            assetRepository = SandboxAssetRepository(
                assetPath = environment.assetsDir.path,
                assetDirs = environment.allModuleAssetDirs + environment.libraryAssetDirs
            ),
            projectResources = environment.projectResources,
        )

    }

}
