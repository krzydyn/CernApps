package sys;

import java.awt.Component;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;

/**
 *
 * @author krzys
 * string,buffers formating helper class
 */
public class StrUtil {
	static final Logger log=Logger.getLogger();
	public final static String RFU="RFU";
	
	public final static String DEG="\u00b0";//176
	public final static String MICRO="\u00b5";
	public final static String CRLF="\r\n";
	
	public final static String UTF_8="UTF-8";
	public final static String UNICODE="UNICODE";
	public final static String WIN1250="CP1250";
	public final static String ISO8859_1="ISO8859-1";
	public final static String ISO8859_2="ISO8859-2";
	
	static final DecimalFormatSymbols decSymbols=new DecimalFormatSymbols(Locale.US);
	static final DecimalFormat decFormat=new DecimalFormat("0.#######",decSymbols);

	//&nbsp;
	//A a C c E e L l N n O o S s X x(zi) Z z(rz)
	protected static char[] unicodeChars= {
		0xa0,
		0x104,0x105,0x106,0x107,0x118,0x119,0x141,0x142,0x143,0x144,0xd3,0xf3,0x15a,0x15b,0x179,0x17a,0x17b,0x17c,
	};
	protected static char[] win1250Chars= {
		0xa0,
		0xa5,0xb9,0xc6,0xe6,0xca,0xea,0xa3,0xb3,0xd1,0xf1,0xd3,0xf3,0x8c,0x9c,0x8f,0x9f,0xaf,0xbf,
	};
	protected static char[] iso8859_2Chars={
		0xa0,
		0xa1,0xb1,0xc6,0xe6,0xca,0xea,0xa3,0xb3,0xd1,0xf1,0xd3,0xf3,0xa6,0xb6,0xaf,0xbf,0xac,0xbc,
	};

	// taken from ISOUtil.java
	private static char ebcdic2ascii[]={
		0x00,0x01,0x02,0x03,0x9c,0x09,0x86,0x7f,0x97,0x8d,0x8e,0x0b,0x0c,0x0d,0x0e,0x0f,
		0x10,0x11,0x12,0x13,0x9d,0x0a,0x08,0x87,0x18,0x19,0x92,0x8f,0x1c,0x1d,0x1e,0x1f,
		0x80,0x81,0x82,0x83,0x84,0x85,0x17,0x1b,0x88,0x89,0x8a,0x8b,0x8c,0x05,0x06,0x07,
		0x90,0x91,0x16,0x93,0x94,0x95,0x96,0x04,0x98,0x99,0x9a,0x9b,0x14,0x15,0x9e,0x1a,
		0x20,0xa0,0xe2,0xe4,0xe0,0xe1,0xe3,0xe5,0xe7,0xf1,0xa2,0x2e,0x3c,0x28,0x2b,0x7c,
		0x26,0xe9,0xea,0xeb,0xe8,0xed,0xee,0xef,0xec,0xdf,0x21,0x24,0x2a,0x29,0x3b,0x5e,
		0x2d,0x2f,0xc2,0xc4,0xc0,0xc1,0xc3,0xc5,0xc7,0xd1,0xa6,0x2c,0x25,0x5f,0x3e,0x3f,
		0xf8,0xc9,0xca,0xcb,0xc8,0xcd,0xce,0xcf,0xcc,0x60,0x3a,0x23,0x40,0x27,0x3d,0x22,
		0xd8,0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0xab,0xbb,0xf0,0xfd,0xfe,0xb1,
		0xb0,0x6a,0x6b,0x6c,0x6d,0x6e,0x6f,0x70,0x71,0x72,0xaa,0xba,0xe6,0xb8,0xc6,0xa4,
		0xb5,0x7e,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7a,0xa1,0xbf,0xd0,0x5b,0xde,0xae,
		0xac,0xa3,0xa5,0xb7,0xa9,0xa7,0xb6,0xbc,0xbd,0xbe,0xdd,0xa8,0xaf,0x5d,0xb4,0xd7,
		0x7b,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0xad,0xf4,0xf6,0xf2,0xf3,0xf5,
		0x7d,0x4a,0x4b,0x4c,0x4d,0x4e,0x4f,0x50,0x51,0x52,0xb9,0xfb,0xfc,0xf9,0xfa,0xff,
		0x5c,0xf7,0x53,0x54,0x55,0x56,0x57,0x58,0x59,0x5a,0xb2,0xd4,0xd6,0xd2,0xd3,0xd5,
		0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,0x38,0x39,0xb3,0xdb,0xdc,0xd9,0xda,0x9f,
	};
	
	private static char ascii2ebcdic[]={
		0x00,0x01,0x02,0x03,0x37,0x2d,0x2e,0x2f,0x16,0x05,0x15,0x0b,0x0c,0x0d,0x0e,0x0f,
		0x10,0x11,0x12,0x13,0x3c,0x3d,0x32,0x26,0x18,0x19,0x3f,0x27,0x1c,0x1d,0x1e,0x1f,
		0x40,0x5a,0x7f,0x7b,0x5b,0x6c,0x50,0x7d,0x4d,0x5d,0x5c,0x4e,0x6b,0x60,0x4b,0x61,
		0xf0,0xf1,0xf2,0xf3,0xf4,0xf5,0xf6,0xf7,0xf8,0xf9,0x7a,0x5e,0x4c,0x7e,0x6e,0x6f,
		0x7c,0xc1,0xc2,0xc3,0xc4,0xc5,0xc6,0xc7,0xc8,0xc9,0xd1,0xd2,0xd3,0xd4,0xd5,0xd6,
		0xd7,0xd8,0xd9,0xe2,0xe3,0xe4,0xe5,0xe6,0xe7,0xe8,0xe9,0xad,0xe0,0xbd,0x5f,0x6d,
		0x79,0x81,0x82,0x83,0x84,0x85,0x86,0x87,0x88,0x89,0x91,0x92,0x93,0x94,0x95,0x96,
		0x97,0x98,0x99,0xa2,0xa3,0xa4,0xa5,0xa6,0xa7,0xa8,0xa9,0xc0,0x4f,0xd0,0xa1,0x07,
		0x20,0x21,0x22,0x23,0x24,0x25,0x06,0x17,0x28,0x29,0x2a,0x2b,0x2c,0x09,0x0a,0x1b,
		0x30,0x31,0x1a,0x33,0x34,0x35,0x36,0x08,0x38,0x39,0x3a,0x3b,0x04,0x14,0x3e,0xff,
		0x41,0xaa,0x4a,0xb1,0x9f,0xb2,0x6a,0xb5,0xbb,0xb4,0x9a,0x8a,0xb0,0xca,0xaf,0xbc,
		0x90,0x8f,0xea,0xfa,0xbe,0xa0,0xb6,0xb3,0x9d,0xda,0x9b,0x8b,0xb7,0xb8,0xb9,0xab,
		0x64,0x65,0x62,0x66,0x63,0x67,0x9e,0x68,0x74,0x71,0x72,0x73,0x78,0x75,0x76,0x77,
		0xac,0x69,0xed,0xee,0xeb,0xef,0xec,0xbf,0x80,0xfd,0xfe,0xfb,0xfc,0xba,0xae,0x59,
		0x44,0x45,0x42,0x46,0x43,0x47,0x9c,0x48,0x54,0x51,0x52,0x53,0x58,0x55,0x56,0x57,
		0x8c,0x49,0xcd,0xce,0xcb,0xcf,0xcc,0xe1,0x70,0xdd,0xde,0xdb,0xdc,0x8d,0x8e,0xdf,
	};
	
	public static String vis(byte[] b,int i,int len){
		byte c;
		if (b==null) return "null";
		StringBuilder bo=new StringBuilder();
		len = Math.min(len,b.length-i);
		for (int ii=0; ii<len; ii++){
			c=b[i+ii];
			if (c<0x20||c>=0x7f||c=='<'||c=='>'||c=='\''||c=='\\')
				bo.append(String.format("<%02X>",c&0xff));
			else bo.append((char)c);
		}
		return bo.toString();
	}
	public static String vis(byte[] b) {return vis(b,0,b.length);}
	public static String vis(String b) {return vis(b,0,b.length());}
	public static String vis(StringBuilder b) {return vis(b.toString(),0,b.length());}
	public static String vis(StringBuilder b,int i, int len) {return vis(b.toString(),i,len);}
	public static String vis(String b,int i,int len){
		char c;
		if (b==null) return "null";
		StringBuilder bo=new StringBuilder();
		len = Math.min(len,b.length()-i);
		for (int ii=0; ii<len; ii++){
			c=b.charAt(i+ii);
			if (c<0x20||c>=0x7f||c=='<'||c=='>'||c=='\''||c=='\\')
				bo.append(String.format("<%02X>",(int)c));
			else bo.append((char)c);
		}
		return bo.toString();		
	}
	public static String hex(byte[] b,int i,int len){
		byte c;
		StringBuilder bo=new StringBuilder();
		len = Math.min(len,b.length-i);
		for (int ii=0; ii < len; ii++){
			c=b[i+ii];
			bo.append(String.format("%02X",c&0xff));
		}
		return bo.toString();
	}
	public static String hex(byte[] b) {return b==null?null:hex(b,0,b.length);}
	public static String hex(String b) {return b==null?null:hex(b,0,b.length());}
	public static String hex(StringBuilder b) {return hex(b.toString(),0,b.length());}
	public static String hex(String b, int i, int len){
		if (b==null) return null;
		int c;
		StringBuilder bo=new StringBuilder();
		for (int ii=0; ii < len && i+ii<b.length(); ii++){
			c=b.charAt(i+ii);
			bo.append(String.format("%02X",c&0xff));
		}
		return bo.toString();
	}
	public static byte[] hexbytes(byte[] b) {return bytes(hex(b));}
	public static int bin(String s,byte[] b,int ofs){
		if (b==null) return 0;
		int l=0;
		try{
			for (; l<s.length(); l+=2) Integer.parseInt(s.substring(l,l+2),16);
		}catch (Exception e) {}
		for (int i=0; i<l; i+=2)
			b[ofs+i/2]=(byte)Integer.parseInt(s.substring(i,i+2),16);
		return l;
	}
	public static byte[] bin(String s){
		if (s==null) return null;
		int l=0;
		try{
			for (; l<s.length(); l+=2) Integer.parseInt(s.substring(l,l+2),16);
		}catch (Exception e) {}
		byte b[]=new byte[l/2];
		for (int i=0; i<l; i+=2)
			b[i/2]=(byte)Integer.parseInt(s.substring(i,i+2),16);
		return b;
	}
	public static byte[] bin(byte[] b) {return bin(string0(b));}
	public static void bcd(String s,byte[] dst,int dstofs){
		for (int i=0; i<s.length(); ++i){
			int x=Integer.parseInt(s.substring(i,i+1),16);
			if ((i&1)!=0) dst[dstofs+i/2]|=(byte)(x&0xf);
			else dst[dstofs+i/2]=(byte)((x<<4)&0xf0);
		}
		if ((s.length()&1)!=0) dst[dstofs+s.length()/2]|=0xf;
	}
	public static String binstr(String b) { return string(bin(b)); }
	public static byte[] bytes(StringBuilder s) {return bytes(s,0,s.length());}
	public static byte[] bytes(StringBuilder s,int offs) {return bytes(s,offs,s.length()-offs);}
	public static byte[] bytes(StringBuilder s,int offs,int len) {return bytes(s,offs,new byte[len],0,len);}
	public static byte[] bytes(StringBuilder s,int soffs,byte[] b,int offs,int len) {
		if (s==null) return null;
		for (int i=0; i<len; ++i)
			b[offs+i]=(byte)s.charAt(i+soffs);
		return b;
	}
	public static byte[] bytes(String s,int soffs,byte[] b,int offs,int len){
		if (s==null) return null;
		return bytes(new StringBuilder(s),soffs,b,offs,len);		
	}
	public static byte[] bytes(String s){
		if (s==null) return null;
		return bytes(new StringBuilder(s),0,s.length());
	}
	public static byte[] bytes(String s,int offs){
		if (s==null) return null;
		return bytes(new StringBuilder(s),offs,s.length()-offs);
	}
	public static byte[] bytes(String s,int offs,int len){
		if (s==null) return null;
		return bytes(new StringBuilder(s),offs,len);
	}
	public static String ebcdic(String b,int offs,int len){
		StringBuilder s=new StringBuilder();
		for (int i=0; i<len; i++){
			char unicode=b.charAt(offs+i);
			s.append(ascii2ebcdic[unicode&0xff]);
		}
		return s.toString();		
	}
	public static String unebcdic(byte[] b,int offs,int len){
		StringBuilder s=new StringBuilder();
		for (int i=0; i<len; i++){
			byte ebcdic=b[offs+i];
			char unicode=ebcdic2ascii[ebcdic&0xff];
			if (unicode<0) throw new RuntimeException("Wrong EBCDIC character "+String.format("%02X",(int)ebcdic));
			s.append(unicode);
		}
		return s.toString();
	}
	public static String translate(String fr,String to,String s){
		return translate(fr, to, s, 0, s.length());
	}
	public static String translate(String fr,String to,String s,int offs,int len){
		StringBuilder b=new StringBuilder(s);
		translate(fr, to, b, offs, len);
		return b.toString();
	}
	public static void translate(String fr,String to,StringBuilder s){
		translate(fr, to, s, 0, s.length());
	}
	public static void translate(String fr,String to,StringBuilder s,int offs,int len){
		if (fr.equalsIgnoreCase(to)) return ;
		char[] cfr=null,cto=null;
		if (UNICODE.equalsIgnoreCase(fr)) cfr=unicodeChars;
		else if (WIN1250.equalsIgnoreCase(fr)) cfr=win1250Chars;
		else if (ISO8859_2.equalsIgnoreCase(fr)) cfr=iso8859_2Chars;
		if (UNICODE.equalsIgnoreCase(to)) cto=unicodeChars;
		else if (WIN1250.equalsIgnoreCase(to)) cto=win1250Chars;
		else if (ISO8859_2.equalsIgnoreCase(to)) cto=iso8859_2Chars;
		if (cfr==null||cto==null) return ;
		int l=cfr.length;
		for (int i=0; i<s.length(); ++i){
			char c=s.charAt(i);
			if (c<0x80) continue;
			for (int j=0; j<l; ++j){
				if (c==cfr[j]) {
					s.setCharAt(i,cto[j]);
					break;
				}
			}
		}
	}
	public static void string(StringBuilder s,byte[] b,int offs,int len){
		for (int i=0; i<len; i++) s.append((char)(b[offs+i]&0xff));
	}
	public static void string(StringBuilder s,byte[] b){
		if (b==null) return ;
		string(s,b,0,b.length);
	}
	public static String string(byte[] b,int offs,int len){
		if (b==null) return null;
		StringBuilder s=new StringBuilder();
		string(s,b,offs,len);
		return s.toString();		
	}
	public static String string(byte[] b){
		if (b==null) return null;
		return string(b,0,b.length);
	}
	public static String string0(byte[] b,int offs,int len){
		if (b==null) return null;
		StringBuilder s=new StringBuilder();
		for (int i=0; i<len && b[offs+i]!=0; i++) s.append((char)(b[offs+i]&0xff));
		return s.toString();
	}
	public static String string0(byte[] b){
		if (b==null) return null;
		return string0(b,0,b.length);
	}
	public static String string0(String s){
		if (s==null) return null;
		if (s.indexOf(0)<0) return s;
		return string0(bytes(s));
	}
	public static String toString(URL loc){
		StringBuilder b=new StringBuilder(loc.getFile());
		for (int i=0; i<b.length(); i++){
			int r=b.indexOf("%",i);
			if (r<0) break;
			i=r;
			try {
				r=Integer.parseInt(b.substring(i+1,i+3),16);
				b.setCharAt(i,(char)r);
				b.delete(i+1,i+3);
			}catch (Exception e) {}
		}
		return b.toString();
	}

	public static void padLeft(StringBuilder b,int i,char c,int l){
		if (b.length()>=i+l) b.delete(i,b.length()-l);
		else while (b.length()<i+l) b.insert(i, c);
	}
	public static void padRight(StringBuilder b,int i,char c,int l){
		if (b.length()>=i+l) b.setLength(i+l);
		else while (b.length()<i+l) b.append(c);
	}
	public static String padLeft(String str,char c,int l){
		if (str.length()>=l) return str.substring(str.length()-l);
		StringBuilder b=new StringBuilder(str);
		while (b.length()<l) b.insert(0, c);
		return b.toString(); 
	}
	public static String padRight(String str,char c,int l){
		if (str.length()==l) return str;
		if (str.length()>l) return str.substring(0,l);
		StringBuilder b=new StringBuilder(str);
		while (b.length()<l) b.append(c);
		return b.toString();
	}
	public static StringBuilder trim(StringBuilder str) {
		return trim(str," \t\n\r");
	}
	public static StringBuilder trim(StringBuilder str,String trc) {
		int i;
		for (i=str.length(); i>0; --i) {
			if (trc.indexOf(str.charAt(i-1))<0) break;
			//if (!Character.isWhitespace(str.charAt(i-1))) break;
		}
		str.setLength(i);
		for (i=0; i<str.length(); ++i) {
			if (trc.indexOf(str.charAt(i))<0) break;
			//if (!Character.isWhitespace(str.charAt(i))) break;
		}
		if (i>0) str.delete(0,i);
		return str;
	}
	public static String trim(String str,String trc) {
		return trim(new StringBuilder(str),trc).toString();
	}
	public static StringBuilder remove(StringBuilder str,String t){
		int i=0; 
		for (i=0; (i=str.indexOf(t,i))>=0; i+=t.length())
			str.delete(i,i+t.length());
		return str;
	}
	public static StringBuilder remove(StringBuilder str,String[] tt){
		for (int i=0; i<tt.length; ++i) remove(str,tt[i]);
		return str;
	}
	public static String remove(String str,String t){
		return remove(new StringBuilder(str),t).toString();
	}
	public static String remove(String str,String[] t){
		return remove(new StringBuilder(str),t).toString();
	}
	public static void replace_chr(StringBuilder str,int idx,int len,String fr,String to){
		int r;
		for (int i=idx; i<idx+len; i++){
			if ((r=fr.indexOf(str.charAt(i)))<0) continue;
			str.replace(i,i+1,to.substring(r,r+1));
		}				
	}
	public static void replace_chr(StringBuilder str,int idx,String fr,String to){
		replace_chr(str, idx, str.length()-idx, fr, to);
	}
	public static void replace_chr(StringBuilder str,String fr,String to){
		replace_chr(str, 0, str.length(), fr, to);
	}
	public static String replace_chr(Object str,String fr,String to){
		if (str==null) return null;
		StringBuilder b=new StringBuilder(str.toString());
		replace_chr(b,fr,to);
		return b.toString();
	}
	public static void replace(StringBuilder str,int idx,int len,String[] fr,String[] to) {
		boolean[] notexist=new boolean[fr.length];
		for (int i=0; i<fr.length; i++) notexist[i]=false;
		for (int i=idx; i-idx<len; ) {
			int bestj=-1;
			int bestp=str.length();
			for (int j=0; j<fr.length; j++) {
				if (notexist[j]) continue;
				int p=str.indexOf(fr[j],i);
				if (p<0) {
					notexist[j]=true;
					continue;
				}
				if (p<bestp) {bestj=j;bestp=p;}
			}
			if (bestj<0) break;
			i=bestp;
			String tostr="";
			if (to!=null && bestj<to.length) tostr=to[bestj];
			str.replace(i,i+fr[bestj].length(),tostr);
			i+=tostr.length();
			len+=tostr.length()-fr[bestj].length();
		}
	}
	public static void replace(StringBuilder str,int idx,String[] fr,String[] to) {
		replace(str, idx, str.length()-idx, fr, to);
	}
	public static void replace(StringBuilder str,String[] fr,String[] to) {
		replace(str,0,str.length(),fr,to);
	}
	
	public static String replace(Object str,char fr,char to){
		if (str==null) return null;
		return str.toString().replace(fr,to);
	}
	public static String replace(Object str,String[] fr,String[] to) {
		StringBuilder b=new StringBuilder(str.toString());
		replace(b,0,b.length(),fr,to);
		return b.toString();
	}
	
	public static Object copyOf(Object src, int idx, int len){
		if (src==null) return null;
		if (!src.getClass().isArray()) throw new RuntimeException("not an array");
		Object dst=Array.newInstance(src.getClass().getComponentType(),len);
		if (dst==null){
			log.error("can't create array for %s[%d]",src.getClass().getName(),len);
			return null;
		}
		System.arraycopy(src, idx, dst, 0, len);
		return dst;
	}
	public static boolean compare(Object src1,Object src2, int len){
		if (!src1.getClass().isArray()) throw new RuntimeException("not an array");
		if (!src2.getClass().isArray()) throw new RuntimeException("not an array");
		if (src1.getClass()!=src2.getClass()) return false;
		//TODO compare elements
		return true;
	}
	public static String implode(String sep,Object src,int n){
		if (src==null) return null;
		if (!src.getClass().isArray() && !(src instanceof List))
			return src.toString();
		StringBuilder b=new StringBuilder();
		if (src instanceof List) {
			List<?> l=(List<?>)src;
			for (Object i : l)
				append(b,i.toString(),sep);
		}
		else if (src instanceof byte[]){
			byte[] x=(byte[])src;
			//log.debug("array byte[%d]",x.length);
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof char[]){
			char[] x=(char[])src;
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof short[]){
			short[] x=(short[])src;
			//log.debug("array short[%d]",x.length);
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof int[]){
			int[] x=(int[])src;
			//log.debug("array int[%d]",x.length);
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof long[]){
			long[] x=(long[])src;
			//log.debug("array long[%d]",x.length);
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof float[]){
			float[] x=(float[])src;
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else if (src instanceof double[]){
			double[] x=(double[])src;
			//log.debug("array double[%d]",x.length);
			if (n<0) n=x.length;
			for (int i=0; i<n; i++)
				append(b,String.valueOf(x[i]),sep);
		}
		else{
			Object[] x=(Object[])src;
			if (n<0) n=x.length;
			for (int i=0; i<n; i++){
				append(b,x[i]==null?"null":x[i].toString(),sep);
			}			
		}
		return b.toString();		
	}
	public static String implode(String sep,Object src){
		return implode(sep,src,-1);
	}
	public static String implode(char sep,Object src){
		return implode(new String(new char[]{sep}),src,-1); 
	}
	public static void append(StringBuilder b,String token,String sep){
		if (token==null) return ;
		if (b.length()>0) b.append(sep);
		b.append(token);
	}

	public static int count(int c, StringBuilder buf) {
		return count(c,buf.toString());
	}
	public static int count(int c, String s) {
		int n=0;
		for (int i=0; i<s.length(); ++i)
			if (s.charAt(i)==c) ++n; 
		return n;
	}
	public static String repeat(String s, int n) {
		StringBuilder b=new StringBuilder();
		repeat(b,s,n);
		return b.toString();
	}
	public static void repeat(StringBuilder b,String s, int n) {
		for (int i=0; i<n; ++i) b.append(s);
	}

	public static String unescape(String src){
		if (src==null) return null;
		return unescape(new StringBuilder(src)).toString();
	}
	public static StringBuilder unescape(StringBuilder buf){
		if (buf==null) return null;
		StringBuilder rep=new StringBuilder(1);
		//log.debug("unescape(%s)",buf.toString());
		for (int r,i=0; i<buf.length(); i+=r){
			char c=buf.charAt(i);r=1;
			if (c=='\\'){
				c=buf.charAt(i+r); ++r;
				if (c=='\\') c='\\';
				else if (c=='n') c='\n';
				else if (c=='r') c='\r';
				else if (c=='b') c='\b';
				else if (c=='t') c='\t';
				else if (c=='f') c='\f';
				else if (c=='u'){ //2byte unicode in hex
					c=(char)Integer.parseInt(buf.substring(i+r,i+r+4),16);
					r+=4;					
				}
				else if (c=='x'){ //1byte ascii in hex
					c=(char)Integer.parseInt(buf.substring(i+r,i+r+2),16);
					r+=2;
				}
				else log.error("unknown escape sequence");
				rep.setLength(0);rep.append(c);
				buf.replace(i,i+r,rep.toString());
				r=rep.length();
			}
		}
		return buf;
	}
	public static String escape(String src,String spec){
		if (src==null) return null;
		return escape(new StringBuilder(src),spec).toString();
	}
	public static StringBuilder escape(StringBuilder buf,String spec){
		StringBuilder rep=new StringBuilder(5);
		for (int r,i=0; i<buf.length(); i+=r){
			char c=buf.charAt(i);r=1;
			rep.setLength(0);
			if (spec!=null){
				if (spec.indexOf(c)>=0) rep.append(String.format("\\u%04x",(int)c));	
			}
			else if (c<0x20){
				if (c=='\n') rep.append("\\n");
				else if (c=='\r') rep.append("\\r");
				else if (c=='\b') rep.append("\\b");
				else if (c=='\t') rep.append("\\t");
				else if (c=='\f') rep.append("\\f");
				else rep.append(String.format("\\u%04x",(int)c));
			}
			else if (c<0x80);
			else if (c<0x100) rep.append(String.format("\\x%02x",(int)c));
			else rep.append(String.format("\\u%04x",(int)c));
			if (rep.length()==0) continue;
			buf.replace(i,i+1,rep.toString());
			r=rep.length();
		}
		return buf;
	}
	public static String format(double v){ return decFormat.format(v); }
	public static String format(double v,int w){
		String s=decFormat.format(v);
		return s.length()<w?s:s.substring(0,w);
	}
	public static String format(int v){ return String.format("%d",v); }
	
	public static String[] csvsplit(String buf,char sp,char qt){
		ArrayList<String> a=new ArrayList<String>();
		StringBuilder b=new StringBuilder();
		int l=buf.length();
		boolean quote=false;
		for (int i=0; i<l; ++i){
			char c=buf.charAt(i);
			if (quote) {
				if (c==qt && (i+1==l || buf.charAt(i+1)==sp)) quote=false;
				else b.append(c);
			}
			else if (c==qt && b.length()==0) quote=true; 
			else if (c==sp){a.add(b.toString());b.setLength(0);}
			else b.append(c);
		}
		if (b.length()>0) a.add(b.toString());
		return a.toArray(new String[]{});
	}
	
	public static int uni2utf(StringBuilder b,int uni){
		  int i=b.length();
		  if (uni < 0x80) b.append((char)uni); //one byte
		  else if (uni < 0x800) // two bytes
		  {
			  b.append((char)(0xc0|((uni>>6)&0x1f)));
			  b.append((char)(0x80|(uni&0x3f)));
		  }
		  else //three bytes
		  {
			  b.append((char)(0xe0|((uni>>12)&0xf)));
			  b.append((char)(0x80|((uni>>6)&0x3f)));
			  b.append((char)(0x80|(uni&0x3f)));
		  }
		  return b.length()-i;
	}
	public static int utf2uni(StringBuilder b,int offs,int[] uni){
		int i=offs,r;
		uni[0]=0;
		r=b.charAt(i)&0xff; ++i;
		if ((r&0x80)==0)//7 bits
			uni[0]=r;
		else if ((r&0xe0)==0xc0){//11 bits
			uni[0]=(r&0x1f)<<6; //5bits
			r=b.charAt(i)&0xff; ++i;
		    uni[0]|=r&0x3f;     //6bits
		}
		else if ((r&0xf0)==0xe0){//16 bits
			uni[0]=(r&0x0f)<<12;//4bits
			r=b.charAt(i)&0xff; ++i;
		    uni[0]|=(r&0x3f)<<6; //6bits
		    r=b.charAt(i)&0xff; ++i;
		    uni[0]|=r&0x3f;      //6bits	
		}
		else if ((r&0xf8)==0xf0){//21 bits
			uni[0]=0; //not supported
			i+=3;
			uni[0]=(r&0x07)<<18;//3bits
			r=b.charAt(i)&0xff; ++i;
		    uni[0]|=(r&0x3f)<<12; //6bits
			r=b.charAt(i)&0xff; ++i;
		    uni[0]|=(r&0x3f)<<6; //6bits
		    r=b.charAt(i)&0xff; ++i;
		    uni[0]|=r&0x3f;      //6bits	
		}
		else uni[0]=-r; //not utf8 sequence
		return b.length()-i;
	}
	static public void hierarchy(StringBuilder b, JComponent c){
		int n=c.getComponentCount();
		b.append(c.getClass().getSimpleName());
		if (n>0){
			b.append("=[");
			for (int i=0; i<n; ++i){
				Component cc=c.getComponent(i);
				if (cc instanceof JComponent)
					hierarchy(b,(JComponent)cc);
				else b.append(cc.getClass().getSimpleName());
				b.append(",");
			}
			b.setLength(b.length()-1);
			b.append("]\n");
		}
	}
}
