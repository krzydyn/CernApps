package caen;

import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.Logger;
/**
 * CaenCrate is CaenNet controller with internal modules
 * like HVModule
 * @author KySoft, Krzysztof Dynowski
 *
 */
public class CaenCrate {
	final public static Logger log=Logger.getLogger();
	final protected static int CMD_IDENT=0;
	private CaenNet net;//CAENet interface
	private int addr=0;  //crate address on caenet (1-99)
	private String name; //crate ident
	protected final ArrayList<CaenModule> modules=new ArrayList<CaenModule>();
	protected final ArrayList<ChangeListener> chglist=new ArrayList<ChangeListener>();

	public void addChangeListener(ChangeListener l) {chglist.add(l);}

	final public void setCaenNet(CaenNet n){net=n;}
	final public void setAddress(int a){addr=a;}
	final public int getAddress(){return addr;}
	final public void setName(String n){name=n;}
	final public String getName(){return name;}

	public int getModulesCount(){return modules.size();}
	public CaenModule getModule(int i){return i<modules.size()?modules.get(i):null;}
	public CaenModule findModule(int slot)
	{
		for (int i=0; i<modules.size(); ++i)
		{
			CaenModule m=modules.get(i);
			if (m.slot==slot) return m;
		}
		return null;
	}

	public int command(int fn,StringBuilder b) {
		return net.command(addr,fn,b);
	}
	public int readIdent(StringBuilder b)
	{
		b.setLength(0);
		int r=net.readIdent(addr,b);
		if (r<0) return r;
		name=b.toString();
		return b.length();
	}
	protected void stateChangedNotify(final CrateChangeEvent ev)
	{
		for (int i=0; i<chglist.size(); ++i)
			chglist.get(i).stateChanged(ev);
	}

	@SuppressWarnings("serial")
	static public class CrateChangeEvent extends ChangeEvent
	{
		public CrateChangeEvent(Object source) { super(source); }
	}
}
