package com.trivial.upv.android.helper.singleton;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Created by jvg63 on 22/01/2017.
 */
public class VolleySingleton {
    private static VolleySingleton volleySingleton = null;

    public static VolleySingleton getInstance(Context context) {
        if (volleySingleton == null)
            synchronized (VolleySingleton.class) {
                if (volleySingleton == null)
                    volleySingleton = new VolleySingleton(context);
            }
        return volleySingleton;
    }

    public static RequestQueue getColaPeticiones() {
        return requestQueue;
    }

    public static ImageLoader getLectorImagenes() {
        return imgReader;
    }

    private static StringRequest stringRequest;

    private static RequestQueue requestQueue;
    private static ImageLoader imgReader;

    private VolleySingleton(Context context) {

        requestQueue = Volley.newRequestQueue(context);

        imgReader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }

            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }
        });
    }
}
