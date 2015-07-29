package caen;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import common.StrUtil;
import common.io.ByteInputStream;
import common.io.ByteOutputStream;

/**
 * CEAN SY527 Universal MultiChannel Power Supply System
 * @author KySoft, Krzysztof Dynowski
 *
 */
public class SY527 extends CaenCrate {
	final protected static int CMD_GETMODINFO=3;
	final protected static int CMD_GETCROCCUP=4; //crate occupation
	final protected static int CMD_GETCRSTATUS=5;//v3.04 general status
	final protected static int CMD_GETHVMAX=6;
	final protected static int CMD_GETHWSTATUS=7;//v2.10 hardware status
	final protected static int CMD_HWRESTART=8;  //v2.10 test&reset
	final protected static int CMD_HWRESTARTOK=9;//v2.10 test&reset if testok

	//channel commands
	final public static int CMD_GetChnSTATUS=1;
	final public static int CMD_GetChnSETTINGS=2;
	final public static int CMD_SetChnV0SET=0x10;
	final public static int CMD_SetChnV1SET=0x11;
	final public static int CMD_SetChnI0SET=0x12;
	final public static int CMD_SetChnI1SET=0x13;
	final public static int CMD_SetChnVMAX=0x14; //vmax software value
	final public static int CMD_SetChnRUP=0x15;
	final public static int CMD_SetChnRDN=0x16;
	final public static int CMD_SetChnTRIP=0x17;
	final public static int CMD_SetChnMF=0x18;   // [6.3.2 MASK & FLAG SETTING]
	final public static int CMD_SetChnNAME=0x19; //v3.04

	//alarm commands
	final protected static int CMD_SETALRSTATUS=0x1a;//v3.04
	final protected static int CMD_CLEARALARM=0x32;//v3.04

	//groups of channels commands
	final protected static int CMD_SETGRPNAME=0x1b;//v3.04
	final protected static int CMD_LISTGROUP=0x40; //v3.04
	final protected static int CMD_READGROUP=0x41; //v3.04
	final protected static int CMD_GRPADDCHN=0x50; //v3.04
	final protected static int CMD_GRPDELCHN=0x51; //v3.04

	//other commands
	final protected static int CMD_FORMATEEP=0x30; //v3.04
	final protected static int CMD_CFORMATEEP=0x31;//v3.04 confirm format
	final protected static int CMD_LOCKKBD=0x33;   //v3.04
	final protected static int CMD_UNLOCKKBD=0x34; //v3.04
	final protected static int CMD_KILLALLCHNS=0x35;//v3.04
	final protected static int CMD_CKILLALLCHNS=0x36;//v3.04 confirm killallchns
	//-------------------------
	//     Sy527 is v3.17
	//-------------------------
	final protected static int CMD_READPON_CHK=0x60; //v3.26 read Power-ON checksum
	final protected static int CMD_READCURR_CHK=0x61;//v3.26 read current checksum

	private int mask=-1;
	private int alarmbits=0;
	private int panelbits=0;

	public HVModule findHVModule(int slot){
		return (HVModule)super.findModule(slot);
	}

	public void readCrateConf(){
		mask=-1; modules.clear();
		StringBuilder b=new StringBuilder();
		if (readCrateStatus(b)<0) return ;
		if (readCrateConf(b)<0) return ;
		log.debug("modules mask %X",mask);
		if (mask==0) return ;
		int m=mask;
		for (int i=0; i<16 && m!=0; ++i){
			if ((m&(1<<i))==0) continue;
			if (readModuleInfo(i,b)<0) break;
			try{
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			byte[] buf=new byte[5];
			dis.read(buf,0,5);
			String name=StrUtil.string0(buf);
			HVModule mod=new HVModule();
			mod.setBoard(this,i,name);
			mod.parseModuleInfo(dis);
			m^=1<<i;
			modules.add(mod);
			log.debug("slot=%d module='%s'",mod.getSlot(),mod.getName());
			}catch (Exception e){
				log.error(e);
				--i; //decrement to make next try
			}
		}
	}
	public int readCrateStatus(StringBuilder b){
		b.setLength(0);
		int r=command(CMD_GETCRSTATUS,b);
		if (r<0) return r;
		DataInputStream ds=new DataInputStream(new ByteInputStream(b));
		try{
			alarmbits=ds.readUnsignedShort();
			panelbits=ds.readUnsignedShort();
			ds.close();
		}catch (Exception e) {}
		log.debug("alarms=%X panel=%X",alarmbits,panelbits);
		return r;
	}
	public int readCrateConf(StringBuilder b){
		b.setLength(0);
		int r=command(CMD_GETCROCCUP,b);
		if (r<0) return r;
		DataInputStream ds=new DataInputStream(new ByteInputStream(b));
		try{
			mask=ds.readUnsignedShort();
			ds.close();
		}catch (Exception e) {}
		return r;
	}
	public int killChannels(){
		StringBuilder b=new StringBuilder();
		int r=command(CMD_KILLALLCHNS,b);
		if (r<0) return r;
		log.debug("killresp %s",StrUtil.vis(b.toString()));
		r=command(CMD_CKILLALLCHNS,b);
		if (r<0) return r;
		log.debug("ckillresp %s",StrUtil.vis(b.toString()));
		return r;
	}
	public int readModuleInfo(int slot,StringBuilder b){
		int r=0;
		if (slot<0 || slot>9) return -1;
		b.setLength(0);
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
		dos.writeShort(slot);
		dos.close();
		r=command(CMD_GETMODINFO,b);
		}catch (Exception e) {log.error(e);}
		return r;
	}
	public int readChnSettings(HVModule m,int chn,StringBuilder b){
		int r=-1;
		int slot=m.getSlot();
		if (slot<0||slot>9||chn>255) {log.error("chn(%d.%d) out of range",slot,chn);return -1;}
		if (b==null) b=new StringBuilder();
		else b.setLength(0);
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
			dos.writeShort((slot<<8)|chn);
			r=command(CMD_GetChnSETTINGS, b);
			dos.close();
			DataInputStream dis=new DataInputStream(new ByteInputStream(b));
			m.parseChnSettings(chn,dis);
		}catch (Exception e) {}
		return r;
	}
	public int readChnStatus(HVModule m,int chn,StringBuilder b){
		int r=-1;
		int slot=m.getSlot();
		if (slot<0||slot>9||chn>255) {log.error("chn(%d.%d) out of range",slot,chn);return r;}
		if (b==null) b=new StringBuilder();
		else b.setLength(0);
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
		dos.writeShort((slot<<8)|chn);
		dos.close();
		if ((r=command(CMD_GetChnSTATUS, b))<0) return r;
		DataInputStream dis=new DataInputStream(new ByteInputStream(b));
		m.parseChnStatus(chn,dis);
		}catch (Exception e) {}
		return r;
	}
	public int setChnParam(int slot,int chn,int par,int v){
		int r=-1;
		if (slot<0||slot>9||chn>255) {log.error("chn(%d.%d) out of range",slot,chn);return -1;}
		if (par<CMD_SetChnV0SET || par>CMD_SetChnMF) return -1;
		log.debug("setParam(slot=%d,chn=%d,p=0x%X) to %X",slot,chn,par,v);
		StringBuilder b=new StringBuilder();
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
		dos.writeShort((slot<<8)|chn);
		dos.writeShort(v);dos.close();
		r=command(par, b);
		log.debug("setPar resp[%d]: %s",b.length(),StrUtil.hex(b.toString()));
		}catch (Exception e) {}
		return r;
	}
	public int setChnName(int slot,int chn,String n){
		int r=-1;
		if (slot<0||slot>9||chn>255) {log.error("chn(%d.%d) out of range",slot,chn);return -1;}
		StringBuilder b=new StringBuilder();
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
		dos.writeShort((slot<<8)|chn);
		byte[] bytes=new byte[11];
		StrUtil.bytes(n,0,bytes,0,Math.min(n.length(),bytes.length));
		dos.write(bytes);dos.write(0);
		dos.close();
		r=command(CMD_SetChnNAME, b);
		}catch (Exception e) {}
		return r;
	}
	public int setMaskFlag(int slot,int chn,int v){
		return setChnParam(slot,chn,CMD_SetChnMF,v);
	}
}
