package caen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import sys.Errno;

import com.io.ByteInputStream;
import com.io.ByteOutputStream;
import com.link.TCP;

/*
 * Low Voltage interface over TCP/IP
 */
public class CaenVME extends TCP {
	final protected static int
		CMD_SWREL=0,     CMD_BRDREL=1, CMD_DISPREAD=2,
		CMD_READ=10,     CMD_WRITE=11,     //single
		CMD_READN=12,    CMD_WRITEN=13,    //multiple
		CMD_BLKREAD=14,  CMD_BLKWRITE=15,  //block
		CMD_XBLKREAD=16, CMD_XBLKWRITE=17, //multiplexed
		CMD_REGREAD=18,  CMD_REGWRITE=19,  //register
		CMD_FLREAD=20,   CMD_FLWRITE=21,   //flash
		//TODO pulser, scaler, input, output, irq
		CMD_LAST=22;

	static class DisplayInfo {
		int addr;
		int data;
		int am;
		int irq;
		boolean ds0,ds1;
		boolean as,iack,wr,lw,berr,sysres,br,gr;
	}
	static public class VmeCommand {
		VmeCommand(int pci,int cr,int fn) {
			this.pci=pci; this.crate=cr; this.func=fn;
		}
		void copyFrom(VmeCommand c) {
			pci=c.pci; crate=c.crate; func=c.func;
		}
		int pci;
		int crate;
		int func;
	}
	private static List<VmeCommand> cmdq=new ArrayList<VmeCommand>();

	private static final char[] emptyhdr=new char[4];
	//send/recv for asynchronous communication (fast)
	public int commandSend(VmeCommand c,StringBuilder b) {
		b.insert(0, emptyhdr);
		b.setCharAt(0,(char)c.pci);
		b.setCharAt(1,(char)c.crate);
		b.setCharAt(2,(char)((c.func>>8)&0xff));
		b.setCharAt(3,(char)(c.func&0xff));
		int r=super.send(b);
		b.delete(0,4);
		if (r>=0) cmdq.add(c);
		return r;
	}
	public int commandRecv(VmeCommand c, StringBuilder b) {
		int r,trycnt=0;
		do{
			r=super.recv(b);
			++trycnt;
		}while (r==-Errno.EAGAIN);
		VmeCommand cmd = cmdq.remove(0);
		c.copyFrom(cmd);
		if (r<0) log.error("recv=%d: %d,%d,%d",r,cmd.pci,cmd.crate,cmd.func);
		else if (trycnt>2) log.error("Resp(%d): %d,%d,%d",trycnt,cmd.pci,cmd.crate,cmd.func);
		return r;
	}

	// Synchronous command (simple but slow)
	public int command(int pci,int cr,int fn,StringBuilder b) {
		if (super.io==null) return -Errno.EABORT;
		VmeCommand cmd=new VmeCommand(pci, cr, fn);
		int r=commandSend(cmd, b);
		if (r<0) return r;
		return commandRecv(cmd, b);
	}

	public int readSWVersion(StringBuilder b) {
		b.setLength(0);
		int r=command(0,0,CMD_SWREL,b);
		if (r<0) return r;
		return b.length();
	}
	public int readBoardVersion(int pci,int cr,StringBuilder b) {
		b.setLength(0);
		int r=command(pci,cr,CMD_BRDREL,b);
		if (r<0) return r;
		return b.length();
	}
	public int readDisplay(int pci,int cr,StringBuilder b) {
		b.setLength(0);
		int r=command(pci,cr,CMD_DISPREAD,b);
		if (r<0) return r;
		return b.length();
	}
	public int read(int pci,int cr,int addr,StringBuilder b) {
		int r=-1;
		b.setLength(0);
		DataOutputStream ds=new DataOutputStream(new ByteOutputStream(b));
		try{
			ds.writeInt(addr);
			ds.close();
			r=command(pci,cr,CMD_READ,b);
		}catch (Exception e) {}
		return r;
	}
	public int write(int pci,int cr,int addr,int d,StringBuilder b) {
		int r=-1;
		b.setLength(0);
		DataOutputStream ds=new DataOutputStream(new ByteOutputStream(b));
		try{
			ds.writeInt(addr);
			ds.writeShort(d);
			ds.close();
			r=command(pci,cr,CMD_WRITE,b);
		}catch (Exception e) {}
		return r;
	}
	static public DisplayInfo parseDisplay(StringBuilder b,DisplayInfo dsp){
		if (dsp==null) dsp=new DisplayInfo();
		DataInputStream ds=new DataInputStream(new ByteInputStream(b));
		try{
			dsp.addr=ds.readInt();
			dsp.data=ds.readInt();
			dsp.am=ds.readInt();
			dsp.irq=ds.readInt();
			dsp.ds0=ds.readByte()>0;
			dsp.ds1=ds.readByte()>0;
			dsp.ds1=ds.readByte()>0;
			dsp.as=ds.readByte()>0;
			dsp.iack=ds.readByte()>0;
			dsp.wr=ds.readByte()>0;
			dsp.lw=ds.readByte()>0;
			dsp.berr=ds.readByte()>0;
			dsp.sysres=ds.readByte()>0;
			dsp.br=ds.readByte()>0;
			dsp.gr=ds.readByte()>0;
			ds.close();
		}catch (Exception e) {log.error(e);}
		return dsp;
	}
}
