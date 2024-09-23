package com.milku.aiproject

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import org.jetbrains.annotations.ApiStatus.Experimental
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPermissionsApi
fun PermissionState.isSecondaryDenied(): Boolean {

    return !status.isGranted && !status.shouldShowRationale
}