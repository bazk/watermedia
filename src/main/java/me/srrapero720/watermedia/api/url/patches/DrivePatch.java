package me.srrapero720.watermedia.api.url.patches;

import me.srrapero720.watermedia.api.url.URLPatch;
import java.net.URL;

public class DrivePatch extends URLPatch {
    private static final String API_KEY = "AIzaSyBiFNT6TTo506kCYYwA2NHqs36TlXC1DMo";
    private static final String API_URL = "https://www.googleapis.com/drive/v3/files/%s?alt=media&key=%s";

    @Override
    public boolean isValid(URL url) {
        return url.getHost().equals("drive.google.com") && url.getPath().startsWith("/file/d/");
    }

    @Override
    public URL patch(URL url) throws PatchingUrlException {
        super.patch(url);
        try {

            // PATH GETTER
            final int start = url.getPath().indexOf("/file/d/") + 8;
            int end = url.getPath().indexOf('/', start);
            if (end == -1) end = url.getPath().length();
            String fileID = url.getPath().substring(start, end);

            return new URL(String.format(API_URL, fileID, API_KEY));
        } catch (Exception e) {
            throw new PatchingUrlException(url, e);
        }
    }
}
