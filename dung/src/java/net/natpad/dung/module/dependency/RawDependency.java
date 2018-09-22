package net.natpad.dung.module.dependency;

import net.natpad.dung.expression.ArrayLiteral;
import net.natpad.dung.expression.IExpressionValue;
import net.natpad.dung.expression.StringLiteralProducer;
import net.natpad.dung.module.IDependency;
import net.natpad.dung.module.task.cc.CcCompilerSettings;
import net.natpad.dung.module.task.cc.CcLinkerSettings;
import net.natpad.dung.module.task.cc.ICcConfigItem;
import net.natpad.dung.run.Session;
import net.natpad.dung.workspace.ModuleRaw;

public class RawDependency implements ICcConfigItem, IDependency {

	private final ModuleRaw raw;
	
	public RawDependency(ModuleRaw raw) {
		this.raw = raw;
	}
	
	@Override
	public IExpressionValue getById(Object id) {
		if (id instanceof String) {
			String st = (String) id;
			if ("libPath".equals(st)) {
				return new ArrayLiteral<String>(raw.libPaths, StringLiteralProducer.instance());
			} else if ("libName".equals(st)) {
				return new ArrayLiteral<String>(raw.libNames, StringLiteralProducer.instance());
			} else if ("headerPath".equals(st)) {
				return new ArrayLiteral<String>(raw.headerPaths, StringLiteralProducer.instance());
			}
		}
		return null;
	}

	@Override
	public IDependency[] children() {
		return null;
	}

	@Override
	public String description() {
		return "Raw[]";
	}

	@Override
	public void setup(Session session, CcCompilerSettings compilerSettings) {
//		System.err.println("raw.headerPath="+raw.headerPath);
		for(String path : raw.headerPaths) {
			String resolved = session.resolveProperties(path);
//			System.err.println("resolved="+resolved);
			compilerSettings.addArgumentOnce("-I"+resolved, false);
		}
	}

	@Override
	public void setup(Session session, CcLinkerSettings linkerSettings) {
		for(String path : raw.libPaths) {
			String resolved = session.resolveProperties(path);
			linkerSettings.addLibSearchPath(resolved);
		}
		for(String name : raw.libNames) {
			String resolved = session.resolveProperties(name);
			linkerSettings.addLibName(resolved);
		}
	}
}
