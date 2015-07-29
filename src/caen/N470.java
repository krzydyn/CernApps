package caen;

import java.io.DataOutputStream;

import common.StrUtil;
import common.io.ByteOutputStream;

/**
 * N470 - 4 Channel Programmable HV Power Supply
 * @author KySoft, Krzysztof Dynowski
 *
 * send(cr=2,code=0x102)[0]: //CMD_GetChnSETTINGS(chn=1)
 * recv[22]:0121 13 0000 00C4 0A41  0000  0000  000F  27   F401 F401 870C
 *           SW  St Vmon Imon V0set I0set V1set I1set Trip Rup  Rdn  Vmax
 *           
 * V0set 0-8000 V
 * I0set 0-3000 uA
 * Trip 0-9999 (s/100)
 * Rup  1-500 V/s
 * Rdn  1-500 V/s
 */
public class N470 extends CaenCrate {
	//channel commands
	final public static int CMD_GETALLCHNS=1;//resp=SW+(Vmon,Imon,Vmax,status}x4
	final public static int CMD_GetChnSETTINGS=2;//resp=SW+(Status,Vmon,Imon,V0set,I0set,V1set,I1set,Trip,Rup,Rdn,Vmax)
	final public static int CMD_SetChnV0SET=3;//no resp
	final public static int CMD_SetChnI0SET=4;//no resp
	final public static int CMD_SetChnV1SET=5;//no resp
	final public static int CMD_SetChnI1SET=6;//no resp
	final public static int CMD_SetChnTRIP=7;//no resp
	final public static int CMD_SetChnRUP=8;//no resp
	final public static int CMD_SetChnRDN=9;//no resp
	final public static int CMD_SetChnON=10;//resp=SW
	final public static int CMD_SetChnOFF=11;//resp=SW
	final protected static int CMD_KILLALLCHNS=12;//no resp

	//alarm commands
	final protected static int CMD_CLEARALARM=13;//no resp

	//other commands
	final protected static int CMD_EnableKBD=14;//no resp
	final protected static int CMD_DisableKBD=15;//no resp
	final protected static int CMD_SetTTL=16;//no resp
	final protected static int CMD_SetNIM=17;//no resp

	public N470()
	{
		HVModule mod=new HVModule();
		mod.setBoard(this,0,"N470");
		modules.add(mod);
	}

	public int readChnAll(StringBuilder b)
	{
		b.setLength(0);
		int r=command(CMD_GETALLCHNS,b);
		if (r<0) return r;
		return r;
	}
	public int readChnSettings(int slot,int chn,StringBuilder b)
	{
		if (slot!=0||chn<0||chn>3) {log.error("%d:%d out of range",slot,chn);return -1;}
		b.setLength(0);
		int r=command(CMD_GetChnSETTINGS|(chn<<8),b);
		if (r<0) return r;
		return r;
	}

	public int setChnParam(int slot,int chn,int par,int v)
	{
		int r=-1;
		if (slot!=0||chn<0||chn>3) {log.error("%d:%d out of range",slot,chn);return -1;}
		if (par<CMD_SetChnV0SET || par>CMD_SetChnOFF) return -1;
		log.debug("setParam(slot=%d,chn=%d,p=%x) to %x",slot,chn,par,v);
		StringBuilder b=new StringBuilder();
		DataOutputStream dos=new DataOutputStream(new ByteOutputStream(b));
		try{
		dos.writeShort(v);dos.close();
		r=command(par|(chn<<8), b);
		log.debug("setPar resp[%d]: %s",b.length(),StrUtil.hex(b.toString()));
		}catch (Exception e) {}
		return r;
	}

}
