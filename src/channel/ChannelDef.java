package channel;
/**
*
* @author KySoft, Krzysztof Dynowski
*
*/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import common.StrUtil;

public class ChannelDef {
	public int id;		//database unique ID
	public String name;	//global unique name
	public String unit;
	public String descr;
	//TODO mapping (name=>pv) should be defined in JCA module
	public String pv; 	//process variable in external system (EPICS)
	public float[] lLimits;//lower limits
	public float[] uLimits;//upper limits
	
	public ChannelDef(int id){this.id=id;}
	public ChannelDef(int id,String nm){this.id=id;pv=name=nm;}
	public ChannelDef(String nm,String descr){id=0;pv=name=nm;this.descr=descr;}
	public ChannelDef(ChannelDef d) {
		id=d.id;
		name=d.name;
		unit=d.unit;
		descr=d.descr;
		pv=d.pv;
		lLimits=d.lLimits; //assume as immutable
		uLimits=d.uLimits; //assume as immutable
		/*
		if (d.uLimits==null) uLimits=null;
		else uLimits=d.uLimits.clone(); //Arrays.copyOf(d.uLimits, d.uLimits.length);
		if (d.lLimits==null) lLimits=null;
		else lLimits=d.lLimits.clone(); //Arrays.copyOf(d.lLimits, d.lLimits.length);
		*/
	}
	public boolean equals(Object o){
		if (o instanceof ChannelDef)
			return id == (((ChannelDef)o).id);
		return false;
	}
	public int hashCode() {
		return id;
	}

	public static float[] parseLimits(String s) {
		if (s==null) return null;
		String[] l=s.split(",");
		float[] fl=new float[l.length];
		for (int i=0; i<fl.length; ++i) {
			try {
				fl[i]=Float.parseFloat(l[i]);
			} catch (NumberFormatException e) {
				fl[i]=Float.NaN;
			}
		}
		return fl;
	}
	public void read(DataInputStream ds) throws IOException{
		id=ds.readInt();
		name=ds.readUTF();
		unit=ds.readUTF().replace("deg. ",StrUtil.DEG);
		descr=ds.readUTF();
	}
	public void write(DataOutputStream ds) throws IOException{
		ds.writeInt(id);
		ds.writeUTF(name);
		ds.writeUTF(unit);
		ds.writeUTF(descr);
	}
	public String toString() { return name; }
	public void writeAlarms(DataOutputStream out) throws IOException {
		writeFloats(out, lLimits);
		writeFloats(out, uLimits);
	}
	public void readAlarms(DataInputStream ds) throws IOException {
		lLimits=readFloats(ds,lLimits);
		uLimits=readFloats(ds,uLimits);
	}

	static float[] readFloats(DataInputStream ds,float[] a) throws IOException {
		int l=ds.readShort();
		if (l==0) a=null;
		else {
			if (a==null || a.length!=l) a=new float[l];
			for (int i=0; i<l; ++i) a[i]=ds.readFloat();
		}
		return a;
	}
	static void writeFloats(DataOutputStream out, float[] a) throws IOException {
		if (a==null) out.writeShort(0);
		else {
			out.writeShort(a.length);
			for (float f:a) out.writeFloat(f);
		}		
	}
}
