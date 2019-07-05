package tat.mukhutdinov.imageloader.lib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.LruCache
import tat.mukhutdinov.imageloader.lib.api.Cache
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SimpleCache(private val context: Context,
                  private val maxCount: Int,
                  private val maxSpace: Int) : Cache {

    init {
        if (maxCount < 0 || maxSpace < 0) {
            throw IllegalArgumentException("macCount and maxSpace have to be not negative")
        }
    }

    private lateinit var memoryCache: LruCache<String, Bitmap>
    private lateinit var cacheDir: File
    private val diskCacheLock = ReentrantLock()
    private val diskCachedNames = mutableListOf<String>()

    private var currentSpace = 0L

    override fun init() {
        memoryCache = object : LruCache<String, Bitmap>(maxCount) {

            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                return bitmap.byteCount / 1024
            }
        }

        diskCacheLock.withLock {
            cacheDir = getDiskCacheDir(DISK_CACHE_DIR)

            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            } else {
                cacheDir.listFiles { file, name ->
                    currentSpace += file.length()
                    diskCachedNames.add(name)
                }
            }
        }
    }

    override fun set(key: String, value: Bitmap) {
        memoryCache.put(key, value)

        diskCacheLock.withLock {
            val hashedKey = hashKeyForDisk(key)
            FileOutputStream(File("${cacheDir.path}/$hashedKey")).use { out ->
                value.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, out)
                currentSpace += out.channel.size()
                diskCachedNames.add(hashedKey)

                while (currentSpace > maxSpace) {
                    val cached = File("${cacheDir.path}/${diskCachedNames[0]}")
                    currentSpace -= cached.length()
                    cached.delete()
                    diskCachedNames.removeAt(0)
                }
            }
        }
    }

    override fun get(key: String): Bitmap? {
        var cached = memoryCache[key]

        if (cached == null) {
            val hashedKey = hashKeyForDisk(key)

            if (diskCachedNames.contains(hashedKey)) {
                cached = diskCacheLock.withLock {
                    BitmapFactory.decodeFile("${cacheDir.path}/$hashedKey")
                }

                memoryCache.put(key, cached)
            }
        }

        return cached
    }

    private fun hashKeyForDisk(key: String): String = try {
        val mDigest = MessageDigest.getInstance("MD5")
        mDigest.update(key.toByteArray())
        mDigest.digest().toHex()
    } catch (e: NoSuchAlgorithmException) {
        key.hashCode().toString()
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external but if not mounted, falls back on internal storage.
    private fun getDiskCacheDir(uniqueName: String): File {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir otherwise use internal cache dir
        val cachePath =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.externalCacheDir?.path
            } else {
                context.cacheDir.path
            }

        return File(cachePath + File.separator + uniqueName)
    }

    companion object {
        private const val DISK_CACHE_DIR = "images"
        private val DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG
        private const val DEFAULT_COMPRESS_QUALITY = 70
    }
}