package com.application.dynamic_app_icon_flutter_plus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MethodCallHandlerImpl(
    private val context: Context
) : MethodChannel.MethodCallHandler {

    companion object {
        private const val MAIN_ACTIVITY_SUFFIX = ".MainActivity"
        private const val DEFAULT_ALIAS_SUFFIX = ".default"
    }

    override fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result
    ) {
        when (call.method) {
            "mSupportsAlternateIcons" -> {
                result.success(true)
            }

            "mGetAlternateIconName" -> {
                runCatching {
                    getCurrentIconName()
                }.onSuccess {
                    result.success(it)
                }.onFailure {
                    result.error("ERROR", it.message, null)
                }
            }

            "mSetAlternateIconName" -> {
                runCatching {
                    val iconName: String? = call.argument("iconName")
                    setIcon(iconName)
                }.onSuccess {
                    result.success(null)
                }.onFailure {
                    result.error("ERROR", it.message, null)
                }
            }

            "mGetAvailableIcons" -> {
                runCatching {
                    getAvailableIcons()
                }.onSuccess {
                    result.success(it)
                }.onFailure {
                    result.error("ERROR", it.message, null)
                }
            }

            else -> result.notImplemented()
        }
    }

    /**
     * Dynamically discovers all available icon aliases
     */
    private fun getAvailableIcons(): List<String> {
        val pm = context.packageManager
        val packageName = context.packageName

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
        }

        val resolveInfos: List<ResolveInfo> =
            pm.queryIntentActivities(
                intent,
                PackageManager.GET_DISABLED_COMPONENTS
            )

        val mainActivityName = packageName + MAIN_ACTIVITY_SUFFIX

        return buildList {
            resolveInfos.forEach { resolveInfo ->
                val componentName = resolveInfo.activityInfo.name

                // Skip main activity
                if (componentName == mainActivityName) return@forEach

                // Match aliases
                if (componentName.startsWith("$packageName$MAIN_ACTIVITY_SUFFIX.")) {

                    val iconName = componentName.substring(
                        packageName.length +
                                MAIN_ACTIVITY_SUFFIX.length + 1
                    )

                    // Skip default alias
                    if (iconName != "default") {
                        add(iconName)
                    }
                }
            }
        }
    }

    /**
     * Gets currently active icon
     */
    private fun getCurrentIconName(): String? {
        val pm = context.packageManager
        val packageName = context.packageName

        val availableIcons = getAvailableIcons()

        val mainActivity = ComponentName(
            packageName,
            packageName + MAIN_ACTIVITY_SUFFIX
        )

        val mainState = pm.getComponentEnabledSetting(mainActivity)

        if (
            mainState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            mainState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        ) {
            return null
        }

        val defaultComponent = ComponentName(
            packageName,
            packageName + MAIN_ACTIVITY_SUFFIX + DEFAULT_ALIAS_SUFFIX
        )

        runCatching {
            pm.getActivityInfo(defaultComponent, 0)

            val defaultState =
                pm.getComponentEnabledSetting(defaultComponent)

            if (defaultState ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            ) {
                return null
            }
        }

        availableIcons.forEach { iconName ->
            val iconComponent = ComponentName(
                packageName,
                "$packageName$MAIN_ACTIVITY_SUFFIX.$iconName"
            )

            val state = pm.getComponentEnabledSetting(iconComponent)

            if (state ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            ) {
                return iconName
            }
        }

        return null
    }

    /**
     * Sets app icon dynamically
     */
    private fun setIcon(iconName: String?) {
        val pm = context.packageManager
        val packageName = context.packageName

        val availableIcons = getAvailableIcons()

        val mainActivity = ComponentName(
            packageName,
            packageName + MAIN_ACTIVITY_SUFFIX
        )

        val defaultComponent = ComponentName(
            packageName,
            packageName + MAIN_ACTIVITY_SUFFIX + DEFAULT_ALIAS_SUFFIX
        )

        // Disable default alias if exists
        runCatching {
            pm.getActivityInfo(defaultComponent, 0)

            pm.setComponentEnabledSetting(
                defaultComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Disable main activity
        pm.setComponentEnabledSetting(
            mainActivity,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all aliases
        availableIcons.forEach { availableIcon ->
            val iconComponent = ComponentName(
                packageName,
                "$packageName$MAIN_ACTIVITY_SUFFIX.$availableIcon"
            )

            pm.setComponentEnabledSetting(
                iconComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Restore default icon
        if (iconName.isNullOrEmpty()) {
            pm.setComponentEnabledSetting(
                mainActivity,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            return
        }

        require(iconName in availableIcons) {
            "Icon '$iconName' not found. Available icons: $availableIcons"
        }

        // Enable selected icon
        val iconComponent = ComponentName(
            packageName,
            "$packageName$MAIN_ACTIVITY_SUFFIX.$iconName"
        )

        pm.setComponentEnabledSetting(
            iconComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}