package net.natpad.dung;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class FileUtil {

    public static void remove(File file, AtomicInteger counter) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File sfile : files) {
                remove(sfile, counter);
            }
        }
        if (file.delete()) {
            counter.incrementAndGet();
        }
    }

    
}
