package net.natpad.dung;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.natpad.dung.sht.ModelLoader;
import net.natpad.dung.workspace.Workspace;
import net.natpad.dung.workspace.WorkspaceMapper;
import net.natpad.sht.model.Model;

public class WorkspaceService {

    public static final String WORKSPACE_EXTENSION = ".dws";

    String currentWorspaceName = "current-workspace" + WORKSPACE_EXTENSION;
    File dungHome;

    public WorkspaceService() {
        String userHome = System.getProperty("user.home");
        dungHome = new File(userHome, ".dung.beetle");
    }

    public Workspace loadWorkspace() {
        File wsFile = new File(dungHome, currentWorspaceName);
        if (!wsFile.exists() || !wsFile.canRead()) {
            throw new NoCurrentWorkspace();
        }

        try {
            return loadWorkspace(wsFile, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasActiveWorspace() {
        File wsFile = new File(dungHome, currentWorspaceName);
        return wsFile.exists() && wsFile.canRead();
    }

    private Workspace loadWorkspace(File wsFile, boolean reportErrors) throws Exception {

        ModelLoader modelLoader = new ModelLoader();
        Model model = modelLoader.parse(new FileInputStream(wsFile), reportErrors);
        WorkspaceMapper mapper = new WorkspaceMapper();

        return mapper.map(model, Workspace.class, null);
    }


    public void select(String idOrName) throws IOException {
        WorkspaceAndName workspaceAndName = findByName(idOrName);
        if (workspaceAndName != null) {
            File cwsfile = new File(dungHome, currentWorspaceName);
            Path path = cwsfile.toPath();
            Path toLinkTo = dungHome.toPath().relativize(workspaceAndName.file.toPath());
            Files.deleteIfExists(path);
            Files.createSymbolicLink(path, toLinkTo);
        }
        else {
            System.out.println("Workspace '" + idOrName + "' does not seem to exist.");
        }
    }

    public void clear(String idOrName) {
        WorkspaceAndName workspaceAndName = findByName(idOrName);
        if (workspaceAndName == null) {
            System.out.println("Workspace '" + idOrName + "' does not seem to exist.");
            return;
        }
        
        
        
    }

    
    public List<WorkspaceAndName> enlist() {

        ArrayList<WorkspaceAndName> result = new ArrayList<WorkspaceAndName>();
        if (dungHome.exists()) {

            File cwFile = new File(dungHome, currentWorspaceName);
            File curWs = null;
            if (cwFile.exists()) {
                try {
                    Path link = Files.readSymbolicLink(cwFile.toPath());
                    curWs = link.toFile();
                    curWs = new File(dungHome, link.toString());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            File[] listFiles = dungHome.listFiles();
            for (File file : listFiles) {
                if (file.isDirectory()) {
                    continue;
                }
                if (file.getName().equals(currentWorspaceName)) {
                    continue;
                }

                try {
                    Workspace ws = loadWorkspace(file, false);
                    if (ws != null) {
                        boolean isSelected = file.equals(curWs);
                        WorkspaceAndName wan = new WorkspaceAndName(ws, file, isSelected);
                        result.add(wan);
                    }
                }
                catch (Throwable t) {
                }
            }
        }
        return result;
    }

    public static class WorkspaceAndName {

        public final Workspace workspace;
        public final File file;
        public final String name;
        public final boolean isSelected;

        public WorkspaceAndName(Workspace workspace, File wsFile, boolean isSelected) {
            this.workspace = workspace;
            this.file = wsFile;
            String n = file.getName();
            this.name = n.substring(0, n.length()-4);
            this.isSelected = isSelected;
        }

    }

    public File getWorkDir(Workspace workspace) {
        return new File(dungHome, workspace.id);
    }

    public WorkspaceAndName findByName(String name) {
        List<WorkspaceAndName> enlisted = enlist();
        for (WorkspaceAndName workspaceAndName : enlisted) {
            if (name.equals(workspaceAndName.name) || workspaceAndName.workspace.id.equals(name)) {
                return workspaceAndName;
            }
        }
        return null;
    }

    public File getDungHome(boolean create) {
        if (!dungHome.exists()) {
            dungHome.mkdirs();
        }
        return dungHome;
    }


}
