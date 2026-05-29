package com.example.cars.util;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.cars.Hellper.DALAppWriteConnection;

public final class AppwriteGlideUrl {

    private AppwriteGlideUrl() {}

    public static GlideUrl withHeaders(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("X-Appwrite-Project", DALAppWriteConnection.appwriteProjectId())
                .addHeader("X-Appwrite-Key", DALAppWriteConnection.appwriteApiKey())
                .build());
    }
}
