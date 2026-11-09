package ru.wexside.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import ru.wexside.misc.DirectoryWatchRegistrar;
import ru.wexside.misc.WavFileVisitor;

public final class CustomSoundLibrary
implements AutoCloseable {
    private final Map<String, byte[]> map2 = new ConcurrentHashMap<String, byte[]>();
    private final AtomicBoolean atomicBoolean;
    private static final float value = 16.0f;
    private final Map<String, List<String>> map3 = new ConcurrentHashMap<String, List<String>>();
    private final Path path;
    private static final float value2 = 1.0f;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "wexside-custom-sounds");
        thread.setDaemon(true);
        return thread;
    });
    private static final float value3 = 1.0f;
    private WatchService watchService;

    public CustomSoundLibrary(Path path) throws IOException {
        this.atomicBoolean = new AtomicBoolean(false);
        this.path = path;
        Files.createDirectories(path, new FileAttribute[0]);
        this.reloadSoundFiles();
        this.startDirectoryWatcher();
    }

    @Override
    public void close() {
        this.atomicBoolean.set(false);
        try {
            if (this.watchService != null) {
                this.watchService.close();
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        this.scheduledExecutorService.shutdownNow();
        this.map2.clear();
        this.map3.clear();
    }

    public void playSound(String string, float f, long l) {
        byte[] byArray = this.map2.get(string);
        if (byArray == null || f <= 0.0f) {
            return;
        }
        this.schedulePlayback(() -> this.playAudioBytes(byArray, f), l);
    }

    public boolean hasCategorySounds(String string) {
        List<String> list = this.map3.get(string);
        return list != null && !list.isEmpty();
    }

    private static float clampVolume(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private float distanceAttenuation(float f) {
        if ((f = Math.max(0.0f, f)) <= 1.0f) {
            return 1.0f;
        }
        if (f >= 16.0f) {
            return 0.0f;
        }
        float f22 = 1.0f / (1.0f + 1.0f * (f - 1.0f));
        float f3 = 1.0f - (f - 1.0f) / 15.0f;
        return CustomSoundLibrary.clampVolume(f22 * f3);
    }

    public List<String> soundsInCategory(String string) {
        ArrayList<String> arrayList = new ArrayList<String>(this.map3.getOrDefault(string, Collections.emptyList()));
        Collections.sort(arrayList);
        return arrayList;
    }

    public void playPositionalSound(String string, double d, double d2, double d3, double d4, double d5, double d6, float f, long l) {
        byte[] byArray = this.map2.get(string);
        if (byArray == null) {
            return;
        }
        double d7 = d - d4;
        double d8 = d2 - d5;
        double d9 = d3 - d6;
        float f2 = (float)Math.sqrt(d7 * d7 + d8 * d8 + d9 * d9);
        float f3 = f * this.distanceAttenuation(f2);
        if (f3 <= 0.0f) {
            return;
        }
        this.schedulePlayback(() -> this.playAudioBytes(byArray, f3), l);
    }

    public boolean containsSound(String string) {
        return this.map2.containsKey(string);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reload() {
        CustomSoundLibrary customSoundLibrary2;
        CustomSoundLibrary customSoundLibrary;
        CustomSoundLibrary customSoundLibrary3 = customSoundLibrary = (customSoundLibrary2 = this);
        synchronized (customSoundLibrary3) {
            try {
                this.reloadSoundFiles();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public List<String> getList() {
        ArrayList<String> arrayList = new ArrayList<String>(this.map3.keySet());
        Collections.sort(arrayList);
        return arrayList;
    }

    private void removeSoundFile(Path path) {
        String string = this.soundIdFromPath(path);
        this.map2.remove(string);
        String string2 = CustomSoundLibrary.categoryFromSoundId(string);
        List<String> list = this.map3.get(string2);
        if (list != null) {
            list.remove(string);
            if (list.isEmpty()) {
                this.map3.remove(string2);
            }
        }
    }

    private void schedulePlayback(Runnable runnable, long l) {
        if (l > 0L) {
            this.scheduledExecutorService.schedule(runnable, l, TimeUnit.MILLISECONDS);
        } else {
            this.scheduledExecutorService.execute(runnable);
        }
    }

    public void loadSoundFile(Path path) {
        try {
            String string2 = this.soundIdFromPath(path);
            this.map2.put(string2, Files.readAllBytes(path));
            String string3 = CustomSoundLibrary.categoryFromSoundId(string2);
            List list = this.map3.computeIfAbsent(string3, string -> new CopyOnWriteArrayList());
            if (!list.contains(string2)) {
                list.add(string2);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static boolean isWavFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav");
    }

    private void registerDirectoryTree(Path path) throws IOException {
        Files.walkFileTree(path, new DirectoryWatchRegistrar(this));
    }

    public void registerDirectory(Path directory) throws IOException {
        directory.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    private void reloadSoundFiles() throws IOException {
        this.map2.clear();
        this.map3.clear();
        Files.walkFileTree(this.path, new WavFileVisitor(this));
    }

    private void playAudioBytes(byte[] byArray, float f) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(byArray)));){
            AudioFormat audioFormat = audioInputStream.getFormat();
            AudioFormat audioFormat2 = CustomSoundLibrary.pcmFormat(audioFormat);
            AudioInputStream audioInputStream2 = audioFormat.matches(audioFormat2) ? audioInputStream : AudioSystem.getAudioInputStream(audioFormat2, audioInputStream);
            Clip clip = (Clip)AudioSystem.getLine(new DataLine.Info(Clip.class, audioFormat2));
            clip.addLineListener(lineEvent -> {
                if (lineEvent.getType() == LineEvent.Type.STOP || lineEvent.getType() == LineEvent.Type.CLOSE) {
                    lineEvent.getLine().close();
                }
            });
            clip.open(audioInputStream2);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl floatControl = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
                float f2 = (float)(20.0 * Math.log10(Math.max(1.0E-4f, f)));
                floatControl.setValue(Math.max(floatControl.getMinimum(), Math.min(floatControl.getMaximum(), f2)));
            }
            clip.start();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static AudioFormat pcmFormat(AudioFormat audioFormat) {
        if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            return audioFormat;
        }
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
    }

    private void startDirectoryWatcher() throws IOException {
        if (!this.atomicBoolean.compareAndSet(false, true)) {
            return;
        }
        this.watchService = FileSystems.getDefault().newWatchService();
        this.registerDirectoryTree(this.path);
        Thread thread = new Thread(() -> {
            while (this.atomicBoolean.get()) {
                try {
                    WatchKey watchKey = this.watchService.take();
                    Path path = (Path)watchKey.watchable();
                    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = watchEvent.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            this.loadDirectorySounds(path);
                            continue;
                        }
                        Path path2 = path.resolve((Path)watchEvent.context());
                        if (Files.isDirectory(path2, new LinkOption[0]) && kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            this.registerDirectoryTree(path2);
                            this.loadDirectorySounds(path2);
                            continue;
                        }
                        if (!CustomSoundLibrary.isWavFile(path2)) continue;
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            this.removeSoundFile(path2);
                            continue;
                        }
                        this.loadSoundFile(path2);
                    }
                    if (watchKey.reset()) continue;
                    break;
                }
                catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
                catch (Exception exception) {
                }
            }
        }, "wexside-custom-sounds-watch");
        thread.setDaemon(true);
        thread.start();
    }

    private String soundIdFromPath(Path path) {
        Path path2 = this.path.relativize(path).normalize();
        return path2.toString().replace('\\', '/').replaceAll("\\.wav$", "");
    }

    private void loadDirectorySounds(Path path) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);){
            for (Path path2 : directoryStream) {
                if (Files.isDirectory(path2, new LinkOption[0]) || !CustomSoundLibrary.isWavFile(path2)) continue;
                this.loadSoundFile(path2);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static String categoryFromSoundId(String string) {
        int n = string.indexOf(47);
        return n < 0 ? string : string.substring(0, n);
    }
}

