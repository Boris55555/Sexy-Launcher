package com.boris55555.sexylauncher

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun RecentAppsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    var runningApps by remember {
        mutableStateOf(
            activityManager.runningAppProcesses.mapNotNull {
                try {
                    val packageInfo = context.packageManager.getPackageInfo(it.processName, 0)
                    val appInfo = packageInfo.applicationInfo
                    // Filter out system apps and self
                    if (appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
                        packageInfo.packageName != context.packageName
                    ) {
                        AppInfo(
                            name = appInfo.loadLabel(context.packageManager).toString(),
                            packageName = it.processName,
                            isSystemApp = false
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Recent Apps", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(runningApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(2.dp, Color.Black),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(app.name, modifier = Modifier.padding(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close App",
                            modifier = Modifier
                                .clickable {
                                    activityManager.killBackgroundProcesses(app.packageName)
                                    // Refresh the list
                                    runningApps = activityManager.runningAppProcesses.mapNotNull {
                                        try {
                                            val packageInfo = context.packageManager.getPackageInfo(it.processName, 0)
                                            val appInfo = packageInfo.applicationInfo
                                            if (appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                                packageInfo.packageName != context.packageName
                                            ) {
                                                AppInfo(
                                                    name = appInfo.loadLabel(context.packageManager).toString(),
                                                    packageName = it.processName,
                                                    isSystemApp = false
                                                )
                                            } else {
                                                null
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
