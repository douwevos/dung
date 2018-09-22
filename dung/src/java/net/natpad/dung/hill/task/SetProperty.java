package net.natpad.dung.hill.task;

import net.natpad.dung.hill.HillSession;

public class SetProperty extends Task {

	public String name;
	public String value;

	
	@Override
	public void run(HillSession session) throws Exception {
		session.properties.put(name, value);
	}

}
