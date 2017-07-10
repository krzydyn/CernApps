package caen;

import sys.Logger;

public class CaenModule {
	final public static Logger log=Logger.getLogger();
	protected int slot;
	protected String name;
	protected CaenCrate crate;
	@Override
	public String toString(){return String.format("%d: %s",slot,name);}
	public String getName(){return name;}
	public int getSlot(){return slot;}
	public void setBoard(CaenCrate cr,int slot,String n){crate=cr;this.slot=slot;name=n;}
}
