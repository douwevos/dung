package net.natpad.dung.workspace;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import net.natpad.dung.FileUtil;
import net.natpad.dung.Template;
import net.natpad.dung.expression.IExpressionValue;
import net.natpad.sht.mapper.MapFlat;
import net.natpad.sht.model.BlockValue;

public class Workspace implements IExpressionValue {

    public String id;

    @MapFlat("modulepath")
    List<String> mpaths = new ArrayList<String>();

    @MapFlat()
    Properties properties = new Properties();

    public Map<String, BlockValue> references = new HashMap<String, BlockValue>();

    public Map<String, Template> templates = new HashMap<String, Template>();

    public Map<String, Module> modules = new HashMap<String, Module>();

    public void addModulePath(String path) {
        mpaths.add(path);
    }

    public Path[] getModulePath() {
        Path[] a = new Path[mpaths.size()];
        for (int idx = 0; idx < a.length; idx++) {
            String m = mpaths.get(idx);
            a[idx] = Paths.get(m);
        }
        return a;
    }

    public String getId() {
        return id;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void putProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public IExpressionValue getById(Object id) {
        return null;
    }
    
    public void clearModules(boolean doAll) {
        ArrayList<File> modDirs = enlistModuleDirs();
        for (File modDir : modDirs) {
            File file = new File(modDir, "dung");
            if (!doAll) {
                file = new File(file, id);
            }
            if (file.exists()) {
                AtomicInteger counter = new AtomicInteger();
                FileUtil.remove(file, counter);
                System.out.println("removed:" + file.getAbsolutePath());
            }
        }
    }
    
    public ArrayList<File> enlistModuleDirs() {
        ArrayList<File> enlisted = new ArrayList<File>();
        Path[] modulePaths = getModulePath();
        for (Path modulePath : modulePaths) {
            enlistModuleDirs(modulePath, enlisted);
        }
        return enlisted;
    }

    public void enlistModuleDirs(Path modulePath, ArrayList<File> enlisted) {
        File baseDir = modulePath.toFile();
        if (!baseDir.exists()) {
            return;
        }

        File[] listFiles = baseDir.listFiles();
        for (File moduleDir : listFiles) {
            if (moduleDir.isDirectory()) {
                enlisted.add(moduleDir);
            }
        }
    }

    
}
