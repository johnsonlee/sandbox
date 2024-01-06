package io.johnsonlee.playground.sandbox

import android.animation.AnimationHandler
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler_Delegate
import android.os.SystemClock_Delegate
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.ide.common.rendering.api.Result
import com.android.ide.common.rendering.api.SessionParams
import com.android.internal.lang.System_Delegate
import com.android.layoutlib.bridge.Bridge
import com.android.layoutlib.bridge.BridgeRenderSession
import com.android.layoutlib.bridge.android.BridgeContext
import com.android.layoutlib.bridge.impl.RenderSessionImpl
import io.johnsonlee.playground.util.Gc
import io.johnsonlee.playground.util.check
import io.johnsonlee.playground.util.getFieldReflectively
import java.io.Closeable
import java.util.concurrent.TimeUnit

class Sandbox(private val environment: Environment) : Closeable {

    private val bridge = environment.newBridge()

    fun run(
        deviceModel: DeviceModel = DeviceModel.PIXEL_5,
        theme: String = DEFAULT_THEME,
        showLayoutBounds: Boolean = true,
        inflate: ((BridgeContext, ViewGroup) -> Unit)? = null
    ): kotlin.Result<RenderData> {
        val sessionParams = SessionParamsBuilder.from(environment)
            .copy(deviceModel = deviceModel)
            .withTheme(theme)
            .build()
        val renderSession = createRenderSession(sessionParams)
        Bridge.prepareThread()
        renderSession.init(sessionParams.timeout).check()

        val context = renderSession.getContext()
        Bitmap.setDefaultDensity(DisplayMetrics.DENSITY_DEVICE_STABLE)
        initializeAppCompatIfPresent(context.layoutInflater)

        val inflateResult = renderSession.inflate().check()
        val bridgeRenderSession = createBridgeRenderSession(renderSession, inflateResult)
        val root = bridgeRenderSession.rootViews.first().viewObject as ViewGroup
        System_Delegate.setBootTimeNanos(0L)

        return try {
            context.withTime(0L) {
            }

            inflate?.invoke(context, root)

            (0 until root.childCount).map(root::getChildAt).forEach {
                it.isShowingLayoutBounds = showLayoutBounds
            }

            context.withTime(0) {
                renderSession.render(true).check()
            }

            kotlin.Result.success(RenderData(
                systemViews = bridgeRenderSession.systemRootViews.toList(),
                rootViews = bridgeRenderSession.rootViews.toList(),
                image = bridgeRenderSession.image,
            ))
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        } finally {
            root.removeAllViews()
            AnimationHandler.sAnimatorHandler.set(null)
            renderSession.release()
            bridgeRenderSession.dispose()
            Bridge.cleanupThread()
        }
    }

    private fun createRenderSession(sessionParams: SessionParams): RenderSessionImpl {
        return RenderSessionImpl(sessionParams).apply {
            setElapsedFrameTimeNanos(0L)
            RenderSessionImpl::class.java.getFieldReflectively("mFirstFrameExecuted").set(this, true)
        }
    }

    private fun createBridgeRenderSession(renderSession: RenderSessionImpl, result: Result): BridgeRenderSession {
        return Class.forName("com.android.layoutlib.bridge.BridgeRenderSession").getDeclaredConstructor(
            RenderSessionImpl::class.java,
            com.android.ide.common.rendering.api.Result::class.java
        ).apply {
            isAccessible = true
        }.newInstance(renderSession, result) as BridgeRenderSession
    }

    private fun <R> BridgeContext.withTime(timeNanos: Long, block: () -> R): R {
        val frameNanos = TIME_OFFSET_NANOS + timeNanos

        System_Delegate.setNanosTime(frameNanos)

        val choreographer = Choreographer.getInstance()
        val mCallbacksRunning = choreographer.javaClass.getFieldReflectively("mCallbacksRunning")

        return try {
            mCallbacksRunning.setBoolean(choreographer, true)

            synchronized(sessionInteractiveData) {
                Handler_Delegate.executeCallbacks()
            }

            val currentTimeMs = SystemClock_Delegate.uptimeMillis()
            sessionInteractiveData.choreographerCallbacks.execute(currentTimeMs, Bridge.getLog())
            block()
        } finally {
            mCallbacksRunning.setBoolean(choreographer, false)
        }
    }

    override fun close() {
        bridge.dispose()
        Gc.gc()
    }

    companion object {
        private val TIME_OFFSET_NANOS = TimeUnit.HOURS.toNanos(1L)
    }

}

@JvmSynthetic
internal const val DEFAULT_THEME = "Theme.AppCompat.Light.NoActionBar"


private fun initializeAppCompatIfPresent(inflater: LayoutInflater) {
    lateinit var appCompatDelegateClass: Class<*>

    try {
        Class.forName("androidx.appcompat.widget.AppCompatDrawableManager").run {
            getMethod("preload").invoke(null)
        }
        appCompatDelegateClass = Class.forName("androidx.appcompat.app.AppCompatDelegate")
    } catch (e: ClassNotFoundException) {
        logger.debug("AppCompat not found on classpath")
        return
    }

    if (inflater.factory == null) {
        inflater.factory2 = object : LayoutInflater.Factory2 {
            val appCompatViewInflaterClass = Class.forName("androidx.appcompat.app.AppCompatViewInflater")
            val createViewMethod = appCompatViewInflaterClass.getDeclaredMethod(
                "createView",
                View::class.java,
                String::class.java,
                Context::class.java,
                AttributeSet::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            ).apply { isAccessible = true }

            override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
                return createViewMethod.invoke(
                    appCompatViewInflaterClass.getConstructor().newInstance(),
                    parent,
                    name,
                    context,
                    attrs,
                    true,
                    true,
                    true,
                    true
                ) as? View
            }

            override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
                return onCreateView(null, name, context, attrs)
            }

        }
    } else {
        if (!appCompatDelegateClass.isAssignableFrom(inflater.factory2.javaClass)) {
            throw IllegalStateException("The LayoutInflater already has a Factory installed so we can not install AppCompat's")
        }
    }
}
