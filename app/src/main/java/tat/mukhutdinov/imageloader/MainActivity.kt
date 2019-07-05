package tat.mukhutdinov.imageloader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.image
import kotlinx.android.synthetic.main.activity_main.next
import tat.mukhutdinov.imageloader.lib.ScopedImageLoader
import tat.mukhutdinov.imageloader.lib.SimpleCache
import tat.mukhutdinov.imageloader.lib.SimpleResizer
import tat.mukhutdinov.imageloader.lib.api.Cache
import tat.mukhutdinov.imageloader.lib.api.Resizer
import tat.mukhutdinov.imageloader.lib.images
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())

        val cache: Cache = SimpleCache(this, 5, 10 * 1024 * 1024 /*10 MB*/)

        val resizer: Resizer = SimpleResizer()

        ScopedImageLoader.instance
            .setResizer(resizer)
            .setCache(cache)

        var i = 0
        image.load(images[0], R.drawable.placeholder)

        next.setOnClickListener {
            if (i >= images.size) {
                i = 0
            }

            image.load(images[++i], R.drawable.placeholder)
        }
    }
}
