package com.miesposa.sadia.features.phone

import android.content.Context
import android.os.StatFs
import android.os.Environment

data class StorageInfo(val totalGb: Double, val usedGb: Double, val freeGb: Double)

class StorageAnalyzer(private val context: Context) {
    fun analyze(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBytes = stat.blockCountLong * blockSize
        val freeBytes = stat.availableBlocksLong * blockSize
        val usedBytes = totalBytes - freeBytes
        fun toGb(bytes: Long) = bytes / (1024.0 * 1024.0 * 1024.0)
        return StorageInfo(toGb(totalBytes), toGb(usedBytes), toGb(freeBytes))
    }
}
