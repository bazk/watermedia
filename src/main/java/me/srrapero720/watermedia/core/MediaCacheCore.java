package me.srrapero720.watermedia.core;

import me.srrapero720.watermedia.IMediaLoader;
import me.srrapero720.watermedia.core.exceptions.AttemptToReloadException;
import me.srrapero720.watermedia.core.exceptions.SafeException;
import me.srrapero720.watermedia.core.exceptions.UnsafeException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static me.srrapero720.watermedia.WaterMedia.LOGGER;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MediaCacheCore {
    private static final Marker IT = MarkerFactory.getMarker("PictureStorage");
    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    private static File dir;
    private static File index;
    private static boolean inited = false;

    public static void init(IMediaLoader modLoader) throws SafeException, UnsafeException {
        if (inited) throw new AttemptToReloadException("Rejected attempt to reload LocalStorage");

        // SETUP
        dir = modLoader.getTempDir().toAbsolutePath().resolve("cache/pictures").toFile();
        index = new File(dir, "indexer");

        // LOGGER
        LOGGER.info(IT, "Mounted PictureStorage on path '{}'", dir.toString());

        if (!dir.exists()) dir.mkdirs();
        if (index.exists()) {
            try (DataInputStream stream = new DataInputStream(new GZIPInputStream(new FileInputStream(index)))) {
                int length = stream.readInt();

                for (int i = 0; i < length; i++) {
                    String url = stream.readUTF();
                    String tag = stream.readUTF();
                    long time = stream.readLong();
                    long expireTime = stream.readLong();
                    Entry entry = new Entry(url, tag.length() > 0 ? tag : null, time, expireTime);
                    ENTRIES.put(entry.getUrl(), entry);
                }
            } catch (Exception e) {
                LOGGER.error(IT, "Failed to load indexes", e);
            }
        }

        inited = true;
    }

    private static File getFile(String url) {
        return new File(dir, Base64.encodeBase64String(url.getBytes()));
    }

    public static void saveFile(String url, String tag, long time, long expireTime, byte[] data) {
        Entry entry = new Entry(url, tag, time, expireTime);
        boolean saved = false;
        OutputStream out = null;
        File file = getFile(entry.url);

        try {
            out = new FileOutputStream(file);
            out.write(data);
            saved = true;
        } catch (Exception e) { LOGGER.error(IT, "Failed to save cache file {}", url, e);
        } finally { IOUtils.closeQuietly(out); }

        // SAVE INDEX FIST
        if (saved && refreshAllIndexOnFile()) ENTRIES.put(url, entry);
        else if (file.exists()) file.delete();
    }

    private static boolean refreshAllIndexOnFile() {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(index)));
            out.writeInt(ENTRIES.size());

            for (Map.Entry<String, Entry> mapEntry : ENTRIES.entrySet()) {
                Entry entry = mapEntry.getValue();
                out.writeUTF(entry.getUrl());
                out.writeUTF(entry.getTag() == null ? "" : entry.getTag());
                out.writeLong(entry.getTime());
                out.writeLong(entry.getExpireTime());
            }

            return true;
        } catch (IOException e) {
            LOGGER.error(IT, "Failed to save cache index", e);
            return false;
        } finally { IOUtils.closeQuietly(out); }
    }

    public static Entry getEntry(String url) { return ENTRIES.get(url); }
    public static void updateEntry(Entry fresh) {
        ENTRIES.put(fresh.url, fresh);
    }
    public static void deleteEntry(String url) {
        ENTRIES.remove(url);
        File file = getFile(url);
        if (file.exists()) file.delete();
    }

    public static final class Entry {
        private final String url;
        private String tag;
        private long time;
        private long expireTime;

        public Entry(String url, String tag, long time, long expireTime) {
            this.url = url;
            this.tag = tag;
            this.time = time;
            this.expireTime = expireTime;
        }

        public void setTag(String tag) { this.tag = tag; }
        public void setTime(long time) { this.time = time; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        public String getUrl() { return url; }
        public String getTag() { return tag; }
        public long getTime() { return time; }
        public long getExpireTime() { return expireTime; }
        public File getFile() { return MediaCacheCore.getFile(url); }
    }
}
