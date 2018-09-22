package net.natpad.dung.workspace;

import java.util.ArrayList;
import java.util.List;

import net.natpad.sht.mapper.MapFlat;
import net.natpad.sht.mapper.MapParent;

@MapParent
public class ModuleRaw extends Module {

	
    @MapFlat("libPath")
	public List<String> libPaths = new ArrayList<>();

    @MapFlat("libName")
	public List<String> libNames = new ArrayList<>();

    @MapFlat("headerPath")
	public List<String> headerPaths = new ArrayList<>();
	
	public ModuleRaw() {
		super.type = ModuleType.RAW;
	}
	
	public void addLibPath(String path) {
		libPaths.add(path);
	}
	
	public void addLibName(String path) {
		libNames.add(path);
	}
	
	public void addHeaderPath(String path) {
		headerPaths.add(path);
	}

}
