package com.register_renegades.totem.disk

import androidx.compose.runtime.Composable

expect class PermissionsManager(callback: PermissionCallback) : PermissionHandler

interface PermissionCallback{
    fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus)
}

@Composable
expect fun createPermissionsManager(callback: PermissionCallback): PermissionsManager