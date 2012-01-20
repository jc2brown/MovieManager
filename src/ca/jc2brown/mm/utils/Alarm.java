package ca.jc2brown.mm.utils;

public class Alarm {
	private boolean waiting;
	private String name;
	
	
	public Alarm(String name) {
		this.name = name;
		waiting = false;
	}	
	
	
	public boolean isWaiting() {
		return waiting;
	}
	public int setWaiting(boolean waiting) {
		int ret = ( this.waiting == waiting ? 0 : 1 );
		this.waiting = waiting;
		return ret;
	}

	public String getName() {
		return name;
	}

}