package caen;

import common.Errno;
import common.connection.link.TCP;

public class CaenNet extends TCP {
	public int command(int addr,int fn,StringBuilder b){
		int trycnt=0;
		b.insert(0,new char[]{(char)addr,(char)((fn>>8)&0xff),(char)(fn&0xff)});
		//b.insert(0,(char)addr);
		//b.insert(1,(char)((fn>>8)&0xff));
		//b.insert(2,(char)(fn&0xff));
		//log.debug("send[%d]: %s",b.length(),StrUtil.hex(b.toString()));
		int r=super.send(b);
		b.delete(0,3);
		if (r<0) {
			return r;
		}
		do{
			r=super.recv(b);
			++trycnt;
		}while (r==-Errno.EAGAIN);
		if (r<0) log.error("recv=%d",r);
		else if (trycnt>2) log.error("resp in %d tries",trycnt);
		return r;
	}
	public int readIdent(int cr,StringBuilder b) {
		b.setLength(0);
		int r=command(cr,CaenCrate.CMD_IDENT,b);
		if (r<0) return r;
		//it happens response is shifted by one byte
		if (r>0 && b.charAt(0)!=0) b.insert(0,(char)0);
		//remove trailing zeros
		while (b.length()>0 && b.charAt(b.length()-1)==(char)0)
			b.deleteCharAt(b.length()-1);
		for (int i=0; i<b.length(); ++i) b.deleteCharAt(i);
		return b.length();
	}
}
