package caen;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import common.StrUtil;

public class HVModule extends CaenModule {
	static private String[] fieldNames=new String[]{"ModNm",
		"Iunit","S/N","Version","Chns",
		"Homog","Vmax","Imax","Rmin","Rmax",
		"Vres","Ires","Vdec","Idec"};
	static private String[] iunits=new String[]{"A","mA",StrUtil.MICRO+"A","nA"};

	public int iunit,sn,ver,chns;
	public int homog,vmax;
	public int imax,rmin,rmax,vres,ires,vdec,idec;

	private final ArrayList<ChannelSettings> channels=new ArrayList<ChannelSettings>();

	static public String[] fields(){return fieldNames;}
	public String[] values(){
		return new String[]{
				String.format("%s",getName()),
				String.format("%d[%s]",iunit,iunits[iunit&3]),
				String.format("%d",sn),
				String.format("%x.%02x",ver>>8,ver&0xff),
				String.format("%d",chns),
				String.format("%02X",homog),
				String.format("%dV",vmax),
				HVModule.scaledValue(imax,idec)+iunits[iunit&3],
				String.format("%dV/s",rmin),
				String.format("%dV/s",rmax),
				HVModule.scaledValue(vres,2)+"V",
				HVModule.scaledValue(ires,2)+iunits[iunit&3],
				String.format("%d",vdec),
				String.format("%d",idec),
		};
	}
	public String toString(){return String.format("%d: %s iunit=%d "+
			"sn=%d ver=%x chns=%d homog=%X vmax=%d imax=%d rmin=%d rmax=%d "+
			"vres=%d ires=%d vdec=%d idec=%d %s hom",
			slot,name,
			iunit,sn,ver,chns,homog,vmax,imax,rmin,rmax,vres,ires,vdec,idec,
			(homog&(1<<17))!=0?"NOT":"IS");}

	public ChannelSettings getChannelSettings(int chn){return channels.get(chn);}

	public void parseModuleInfo(DataInputStream dis) throws IOException {
		iunit=dis.readUnsignedByte();//Current units(0=A,1=mA,2=uA,3=nA
		sn=dis.readUnsignedShort();
		ver=dis.readUnsignedShort();
		dis.skip(20);//10 words
		chns=dis.readUnsignedByte();
		homog=dis.readInt();
		vmax=dis.readInt();
		imax=dis.readUnsignedShort();
		rmin=dis.readUnsignedShort();//Ramp min
		rmax=dis.readUnsignedShort();//Ramp max
		vres=dis.readUnsignedShort();
		ires=dis.readUnsignedShort();
		vdec=dis.readUnsignedShort();//0..2
		idec=dis.readUnsignedShort();//0..2
		for (int i=0; i<chns; ++i) channels.add(new ChannelSettings());
	}
	public void parseChnSettings(int chn,DataInputStream dis) throws IOException {
		ChannelSettings cs;
		if (chn<channels.size()) cs=channels.get(chn);
		else{
			//if (chn>channels.size()) return ;
			//channels.add(cs=new ChannelSettings());
			return ;
		}
		byte[] buf=new byte[12];
		dis.read(buf,0,12); //12
		cs.name=StrUtil.string0(buf);
		cs.v0set=dis.readInt();//4 _ 16
		cs.v1set=dis.readInt();//4 _ 20
		cs.i0set=dis.readUnsignedShort();//2
		cs.i1set=dis.readUnsignedShort();//2
		cs.vmax=dis.readUnsignedShort();//2
		cs.rup=dis.readUnsignedShort();//2
		cs.rdn=dis.readUnsignedShort();//2
		cs.trip=dis.readUnsignedShort();//2
		dis.skip(2);//2
		cs.flag=dis.readUnsignedShort();//2 _ 36
	}
	public void parseChnStatus(int chn,DataInputStream dis) throws IOException{
		ChannelSettings cs;
		if (chn<channels.size()) cs=channels.get(chn);
		else return ;
		cs.vread=dis.readInt();
		cs.hvmax=dis.readUnsignedShort();
		cs.iread=dis.readUnsignedShort();
		cs.status=dis.readUnsignedShort();
	}

	public static String scaledValue(int v,int dec){
		float s=(float)Math.pow(10,dec);
		if (dec<0) dec=0;
		String fmt="%."+dec+"f";
		return String.format((Locale)null,fmt,v/s);
	}

	public static class ChannelSettings{
		static private String[] fieldNames=new String[]{"ChnNm",
			"V0set","V1set","I0set","I1set",
			"Vmax","Rup V/s","Rdn V/s","Trip s","Flag",
			"Vread","Iread","HVmax","Staus"};
		//channel settings
		public String name;
		public int v0set,v1set;
		public int i0set,i1set;
		public int vmax;
		public int rup,rdn;
		public int trip; //time of "overcurrent" in tenth of a second
		/** Mask and Flag Word */
		public int flag; //6.4.5 Tab. 34 : Flag Structure

		//channel status
		public int vread;//Vmon
		public int hvmax;
		public int iread;//Imon
		/** Channel Status Word */
		public int status;

		public static String[] fields() { return fieldNames; }
		public static void fields(List<String> f) {
			for (int i=0; i<fieldNames.length; ++i)
				f.add(fieldNames[i]);
		}

		public boolean equalSettings(ChannelSettings cs)
		{
			if (name!=null && cs.name!=null && !name.equals(cs.name))
				return false;
			return v0set==cs.v0set && v1set==cs.v1set &&
				i0set==cs.i0set && i1set==cs.i1set &&
				vmax==cs.vmax && rup==cs.rup && rdn==cs.rdn && trip==cs.trip;
		}
		public void copySettings(ChannelSettings cs)
		{
			cs.name=name;
			cs.v0set=v0set; cs.v1set=v1set;
			cs.i0set=i0set; cs.i1set=i1set;
			cs.vmax=vmax; cs.rup=rup; cs.rdn=rdn; cs.trip=trip;
		}

		public String[] values(HVModule m) {
			StringBuilder st=new StringBuilder();
			//Channel Status Word (p22, 6.4.4 p113)
			if ((status&(1<<0))==0)st.append("N/A");
			else {
				//1,2 don't care
				//if ((status&(1<<3))!=0) st.append("Dlc");//delivers current
				//if ((status&(1<<4))!=0) st.append("Edis");//external disable
				if ((status&(1<<5))!=0) st.append("Tri,");//internal trip
				if ((status&(1<<6))!=0) st.append("Kil,");//killed
				//7 don't care
				if ((status&(1<<8))!=0) st.append("Vm,"); //V max
				if ((status&(1<<9))!=0) st.append("Trx,");//external trip
				if ((status&(1<<10))!=0) st.append("Ov,");//overvoltage
				if ((status&(1<<11))!=0) st.append("Uv,");//undervoltage
				if ((status&(1<<12))!=0) st.append("Oc,");//overcurrent
				if ((status&(1<<13))!=0) st.append("Rdn,");//ramping down
				if ((status&(1<<14))!=0) st.append("Rup,");//ramping up
				if ((status&(1<<15))==0) st.append("OFF,");//off
				else if (st.length()==0) st.append("ON,");//on
				st.deleteCharAt(st.length()-1);
			}

			//- if Mask bit =0 the corresponding parameter maintains the old value;
			//- if Mask bit =1 The corresponding parameter will take the value
			//indicated in the corresponding Flag bit.
			
			//Mask and Flag Word (p21,p115)
			StringBuilder fl=new StringBuilder();
			fl.append(String.format("%X|",flag));
			//flags
			//0 don't care
			/*if ((flag&(1<<1))!=0) fl.append("Etr");
			//2 don't care
			if ((flag&(1<<3))!=0) fl.append("Pwr");
			if ((flag&(1<<4))!=0) fl.append("Psw");
			if ((flag&(1<<5))!=0) fl.append("Pwd");
			if ((flag&(1<<6))!=0) fl.append("On");else fl.append("Off");
			if ((flag&(1<<7))!=0) fl.append("Pon");
			*/
			//masks
			//fl.append(String.format("%02X:",(flag>>8)&0xff));
			//8 don't care
			if ((flag&(1<<9))!=0) fl.append("Trx,");
			//10 don't care
			if ((flag&(1<<12))!=0) fl.append("Psw,");
			if ((flag&(1<<13))!=0) fl.append("Pwdn=Rdn,"); else fl.append("Pwdn=Kil,");
			if ((flag&(1<<14))!=0) fl.append("On=en,"); else fl.append("On=di,");
			if ((flag&(1<<15))!=0) fl.append("Pw=On,"); else fl.append("Pw=Off,");
			if ((flag&(1<<11))!=0) fl.append("ON,"); else fl.append("OFF,");
			fl.deleteCharAt(fl.length()-1);

			return new String[]{name,
					scaledValue(v0set,m.vdec),
					scaledValue(v1set,m.vdec),
					scaledValue(i0set,m.idec),
					scaledValue(i1set,m.idec),
					String.format("%d",vmax),
					String.format("%d",rup),
					String.format("%d",rdn),
					scaledValue(trip,1),//trip is in tenth of sec.
					String.format("%s",fl.toString()),
					scaledValue(vread,m.vdec),
					scaledValue(iread,m.idec),
					//scaledValue(hvmax,m.vdec),(manual mistake?)
					String.format("%d",hvmax),
					String.format("%s",st.toString()),
			};
		}
		public void values(HVModule m,Vector<String> v) {
			String[] vs=values(m);
			for (int i=0; i<vs.length; ++i)
				v.add(vs[i]);
		}

		public String toString(){
			return String.format("n='%s' v0=%d v1=%d i0=%d i1=%d vmax=%d",
					name,v0set,v1set,i0set,i1set,vmax);
		}
		public boolean isTripped() {
			if ((flag&(1<<14))==0) return false; //ON not enabled
			if ((status&(1<<0))==0) return false; //no module
			if ((status&(1<<5))!=0) return true; //internal
			if ((status&(1<<9))!=0) return true; //external
			return false;
		}
	}
}
