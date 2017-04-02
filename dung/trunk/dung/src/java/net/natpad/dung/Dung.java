package net.natpad.dung;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.natpad.dung.WorkspaceService.WorkspaceAndName;
import net.natpad.dung.hill.HillInstaller;
import net.natpad.dung.module.IDependency;
import net.natpad.dung.module.Module;
import net.natpad.dung.module.task.LogLevel;
import net.natpad.dung.run.Context;
import net.natpad.dung.workspace.Workspace;

public class Dung {

    WorkspaceService workspaceService = new WorkspaceService();
    
    LogLevel logLevel = LogLevel.INFO;
    boolean reverse = false;
    boolean nodeps = false;
    Action action = Action.NONE;
    String module = null;
    ArrayList<String> targets = new ArrayList<String>();

    public static void main(String[] args) {
        Dung dung = new Dung();
        dung.run(args);
    }

    private void run(String[] args) {
        StreamHelper.setup();
        boolean argsValid = false;
        try {
            parseArgs(args);
            argsValid = true;
            switch (action) {
                case BUILD:
                    runBuild();
                    break;
                case INSTALL_WORKSPACE:
                    installWorkspace();
                    break;
                case SELECT_WORKSPACE:
                    selectWorkspace();
                    break;
                case LIST_MODULES:
                    listModules();
                    break;
                case LIST_WORKSPACES:
                    listWorkspaces();
                    break;
                case REMOVE_WORKSPACE:
                    removeWorkspace();
                    break;
                case CLEAR_WORKSPACE :
                    clearWorkspace(false);
                    break;
                case PURGE_WORKSPACE :
                    clearWorkspace(true);
                    break;
                case DEPENDENCY_TREE :
                    dependencyTree();
                    break;
                default:
                    dumpUsage();

            }
        }
        catch (Throwable t) {
            if (logLevel==LogLevel.DEBUG) {
                t.printStackTrace();
            } else {
                System.out.println(""+t.getMessage());
            }
            if (!argsValid) {
                dumpUsage();
            }
        }
    }



    private void parseArgs(String[] args) {
        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            String arg = args[argIdx];
            if (arg.startsWith("-")) {
                for (int chidx = 1; chidx < arg.length(); chidx++) {
                    char och = arg.charAt(chidx);
                    switch (och) {
                        case 'v':
                            increaseLogLevel();
                            break;
                        case 'n':
                            nodeps = true;
                            break;
                        case 'r':
                            reverse = true;
                            break;
                    }
                }
            } else if (action==Action.NONE) {
                if ("lm".equals(arg) || "list-modules".equals(arg)) {
                    setAction(Action.LIST_MODULES);
                }
                else if ("lw".equals(arg) || "list-workspaces".equals(arg)) {
                    setAction(Action.LIST_WORKSPACES);
                }
                else if ("install".equals(arg)) {
                    setAction(Action.INSTALL_WORKSPACE);
                }
                else if ("select".equals(arg)) {
                    setAction(Action.SELECT_WORKSPACE);
                }
                else if ("remove".equals(arg)) {
                    setAction(Action.REMOVE_WORKSPACE);
                }
                else if ("clear".equals(arg)) {
                    setAction(Action.CLEAR_WORKSPACE);
                }
                else if ("purge".equals(arg)) {
                    setAction(Action.PURGE_WORKSPACE);
                }
                else if ("tree".equals(arg)) {
                    setAction(Action.DEPENDENCY_TREE);
                }
                else if ("build".equals(arg)) {
                    setAction(Action.BUILD);
                }
                else {
                    if (action == Action.NONE) {
                        setAction(Action.BUILD);
                    }
                    targets.add(arg);
                }
            } else {
                targets.add(arg);
            }
        }
    }

    void setAction(Action action) {
        if (this.action != Action.NONE && action != this.action) {
            throw new RuntimeException("can only run one action");
        }
        this.action = action;
    }

    void increaseLogLevel() {
        switch (logLevel) {
            case INFO:
                logLevel = LogLevel.DEBUG;
                break;
            case DEBUG:
                logLevel = LogLevel.VERBOSE;
                break;
        }
    }

    private void dumpUsage() {
        System.out.println("dung [action] [flags,...]");
        System.out.println("");
        System.out.println("  action");
        System.out.println("     [build] <module> [target,...]  build module");
        System.out.println("     lm,list-modules                list all modules in the active workspace");
        System.out.println("     tree <module>                  show dependency tree for module");
        System.out.println("");
        System.out.println("     lw,list-workspaces             list all installed workspaces");
        System.out.println("     install <.hil-file>            install workspace");
        System.out.println("     select <name|id>               select workspace");
        System.out.println("     remove <name|id>               remove workspace");
        System.out.println("     clear [name|id]                clear workspace (keep builds from other workspaces)");
        System.out.println("     purge [name|id]                purge all dung files from workspace");
        System.out.println("");
        System.out.println("  flags");
        System.out.println("     -v         increase verbosity level");
        System.out.println("     -r         run dependencies reverse (build only)");
        System.out.println("     -n         do not run dependencies (build only)");

    }

    private void runBuild() throws Exception {
        Workspace workspace = workspaceService.loadWorkspace();
        module = targets.remove(0);
        Context context = new Context(workspace, module, workspaceService.getWorkDir(workspace));
        context.setLogLevel(logLevel);
        context.build(targets, reverse, nodeps);
    }

    private void runClearModules() throws Exception {
        Workspace workspace = workspaceService.loadWorkspace();
        workspace.clearModules(true);
    }

    public void enlistModules(Path modulePath, ArrayList<String> enlisted) {
        File baseDir = modulePath.toFile();
        if (!baseDir.exists()) {
            return;
        }

        File[] listFiles = baseDir.listFiles();
        for (File moduleDir : listFiles) {
            if (moduleDir.isDirectory()) {
                enlistModulesInDir(moduleDir, enlisted);
            }
        }
    }

    private void enlistModulesInDir(File moduleDir, ArrayList<String> enlisted) {
        File[] listFiles = moduleDir.listFiles();
        for (File posDunFile : listFiles) {
            if (posDunFile.getName().endsWith(".dun")) {
                String fname = posDunFile.getName();
                fname = fname.substring(0, fname.length() - 4);
                if ("build".equals(fname)) {
                    enlisted.add(moduleDir.getName());
                }
                else {
                    enlisted.add(moduleDir.getName() + "#" + fname);
                }
            }
        }
    }


    private void installWorkspace() throws FileNotFoundException, IOException, Exception {
        HillInstaller installer = new HillInstaller();
        installer.doInstall(targets.get(0));
    }

    private void listModules() {
        Workspace workspace = workspaceService.loadWorkspace();

        ArrayList<String> enlisted = new ArrayList<String>();
        Path[] modulePaths = workspace.getModulePath();
        for (Path modulePath : modulePaths) {
            enlistModules(modulePath, enlisted);
        }

        Collections.sort(enlisted);
        for (String mn : enlisted) {
            System.out.println("  # " + mn);
        }

    }

    private void listWorkspaces() {
        List<WorkspaceAndName> enlist = workspaceService.enlist();
        for (WorkspaceAndName workspaceAndName : enlist) {
            StringBuilder buf = new StringBuilder();

            buf.append(workspaceAndName.name);
            while (buf.length() < 25) {
                buf.append(' ');
            }
            String id = workspaceAndName.workspace.getId();
            if (id != null) {
                buf.append(id);
            }

            if (workspaceAndName.isSelected) {
                buf.append('*');
            }
            else {
                buf.append(' ');
            }

            System.out.println(buf.toString());
        }

    }

    private void selectWorkspace() throws IOException {
        if (targets.isEmpty()) {
            throw new RuntimeException("Expected id or name of workspace as an argument");
        }
        String idOrName = targets.get(0);
        workspaceService.select(idOrName);
    }
    
    private void clearWorkspace(boolean purge) {
        String idOrName = targets.isEmpty() ? null : targets.get(0);
        Workspace workspace = null;
        if (idOrName==null) {
            workspace = workspaceService.loadWorkspace();
            
        } else {
            WorkspaceAndName workspaceAndName = workspaceService.findByName(idOrName);
            workspace = workspaceAndName.workspace;
        }
        workspace.clearModules(purge);
    }

    private void removeWorkspace() {
        if (targets.isEmpty()) {
            throw new RuntimeException("Expected id or name of workspace as an argument");
        }
        String idOrName = targets.get(0);
        WorkspaceAndName workspaceAndName = workspaceService.findByName(idOrName);
        if (workspaceAndName!=null) {
            workspaceAndName.workspace.clearModules(false);
        }
        workspaceAndName.file.delete();
    }


    
    private void dependencyTree() throws IOException {
        if (targets.isEmpty()) {
            throw new RuntimeException("Expected name of module as an argument");
        }
        Workspace workspace = workspaceService.loadWorkspace();
        module = targets.remove(0);
        Context context = new Context(workspace, module, workspaceService.getWorkDir(workspace));
        context.loadModuleAndResolveDependencies();
        
        System.out.println("dumping tree");
        treeOut(context.rootModule, "");
        
    }

    
    private void treeOut(Module rootModule, String prefix) {
        Map<String, Set<Module>> contains = new HashMap<String, Set<Module>>();
        collectTree(contains, rootModule);
        treeOut(contains, rootModule, "", "");
    }

    private void treeOut(Map<String, Set<Module>> contains, Module module, String prefixSelf, String prefixChildren) {
        ArrayList<Module> children = new ArrayList<Module>();
        for (IDependency dependency : module.dependencies) {
            if (dependency instanceof Module) {
                Module m = (Module) dependency;
                children.add(m);
            }
        }
        
        
        ListIterator<Module> listIterator = children.listIterator();
        while(listIterator.hasNext()) {
            Module child = listIterator.next();
            for (Module totest : children) {
                if (totest!=child) {
                    Set<Module> testModuleSet = contains.get(totest.moduleFileDescr.module.name);
                    if (testModuleSet.contains(child)) {
                        listIterator.remove();
                        break;
                    }
                }
            }
        }
        
//        Set<Module> set = contains.get(module.moduleFileDescr.module.name);
//        StringBuilder buf = new StringBuilder();
//        for(Module mo : set) {
//            buf.append(", "+mo.moduleFileDescr.module.name);
//        }
//        
//        System.out.println(prefixSelf+module.moduleFileDescr.module.name+" :: "+buf);
        
        System.out.println(prefixSelf+module.moduleFileDescr.module.name);

        
        listIterator = children.listIterator();
        String spSelf = prefixChildren + "├";
        String spChildren = prefixChildren + "│";
        while(listIterator.hasNext()) {
            Module child = listIterator.next();
            if (!listIterator.hasNext()) {
                spSelf = prefixChildren + "└";
                spChildren = prefixChildren + " ";
            }
            treeOut(contains, child, spSelf, spChildren);
        }
        
        
    }

    private Set<Module> collectTree(Map<String, Set<Module>> contains, Module module) {
        Set<Module> result = contains.get(module.moduleFileDescr.module.name);
        if (result!=null) {
            return result;
        }
        HashSet<Module> mdcontains = new HashSet<Module>();
        contains.put(module.moduleFileDescr.module.name, mdcontains);
        for (IDependency dependency : module.dependencies) {
            if (dependency instanceof Module) {
                Module m = (Module) dependency;
                mdcontains.add(m);
                Set<Module> sub = collectTree(contains, m);
                mdcontains.addAll(sub);
            }
        }
        return mdcontains;
    }

    
    
}
