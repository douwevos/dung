package net.natpad.dung.thread;

import java.util.ArrayList;

import net.natpad.dung.module.task.ExecAction;

public class ThreadPool {

    	ArrayList<ExecAction> actionQueue = new ArrayList<ExecAction>();
    	ActionHandler handlers[];
    	public int exitcode;
    	
    	public ThreadPool() {
    		Runtime runtime = Runtime.getRuntime();
            int nrOfProcessors = runtime.availableProcessors();

            handlers = new ActionHandler[nrOfProcessors+2];
//            handlers = new ActionHandler[1];
            
    		for(int idx=0; idx<handlers.length; idx++) {
    			handlers[idx] = new ActionHandler(this);
    			new Thread(handlers[idx]).start();
    		}
		}
    	
    	
    	public void postAction(IAction action) {
    		while(true) {
    			synchronized (this) {
    				for(int idx=0; idx<handlers.length; idx++) {
    					synchronized (handlers[idx]) {
    						if (handlers[idx].exitCode!=0) {
    							exitcode = handlers[idx].exitCode;
    							return;
    						}
							if (handlers[idx].action == null) {
								handlers[idx].postAction(action);
								return;
							}
						} 
    				}
    				try {
						this.wait(5000);
					} catch (InterruptedException e) {
					}
				}
    		}
    	}
    	
    	
    	public void finish() {
    		boolean keep_alive = true;
    		while(keep_alive) {
    			synchronized(this) {
    				keep_alive = false;
    				for(int idx=0; idx<handlers.length; idx++) {
    					synchronized (handlers[idx]) {
    						if (handlers[idx].exitCode!=0) {
    							exitcode = handlers[idx].exitCode;
    						}
    						
							if (handlers[idx].action != null) {
								keep_alive = true;
							} else {
								handlers[idx].stopit = true;
							}
						} 
    				}
    				if (keep_alive) {
	    				try {
							this.wait(5000);
						} catch (InterruptedException e) {
						}
    				}
				}
    		}
    	}
    	
    }