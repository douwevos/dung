package net.natpad.dung.thread;

public class ActionHandler implements Runnable {
	
	Object notifyme;
	IAction action;
	boolean stopit = false;
	int exitCode;
	
	public ActionHandler(Object notifyme) {
		this.notifyme = notifyme;
		exitCode = 0;
	}
	
	public synchronized void postAction(IAction action) {
		this.action = action;
		notifyAll();
	}
	
	public void run() {
		IAction torun = null;
		boolean donotify = false;
		while(!stopit) {
			synchronized(this) {
				if (action!=null) {
					torun = action;
				} else { 
					try {
						wait(2000);
					} catch (InterruptedException e) {
					}
				}
			}
			if (torun!=null) {
				exitCode = torun.runAction();
				torun = null;
    			synchronized(this) {
					action = null;
					donotify = true;
    			}
			}
			
			if (donotify) {
				synchronized (notifyme) {
					notifyme.notifyAll();
				}
			}
		}
	}
}