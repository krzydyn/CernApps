package sys;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.io.IOUtils;

//TODO Logger hierarchy
//     Logger("") is root, other inherits its properties
//TODO use Formatter to prepare output
//TODO use Handler to store output
//TODO use queue of log messages for efficiency of open/close a file
public class Logger {
	final static SimpleDateFormat dtFull=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	final static SimpleDateFormat dtShort=new SimpleDateFormat("HH:mm:ss.SSS");

	static Logger logger=null;
	static SimpleDateFormat dt=dtFull;

	static java.util.logging.Formatter defaultFmt=new java.util.logging.Formatter(){
		@Override
		public String format(LogRecord r) {
			String s=String.format("%s %s\n",dt.format(new Date(r.getMillis())),r.getMessage());
			Throwable e=r.getThrown();
			if (e==null) return s;
			StringBuilder str=new StringBuilder(s);
			str.append(e.getClass().getName()+": "+e.getMessage()+"\n");
			StackTraceElement trace[]=e.getStackTrace();
			int i0=0;
			for (int i=0; i<trace.length; ++i){
				StackTraceElement t=trace[i];
				if (t.getLineNumber()>0) {i0=i;break;}
			}
			for (int i=i0; i<trace.length; ++i){
				StackTraceElement t=trace[i];
				//str.append("\t"+trace[i].toString()+"\n");
				if (t.getLineNumber()<0) str.append("\t"+t.getClassName()+" (no source)\n");
				else str.append("\t"+t.getClassName()+" ("+t.getFileName()+":"+t.getLineNumber()+")\n");
			}
			while ((e=e.getCause())!=null){
				str.append("Caused by: "+e.toString()+"\n");
				trace=e.getStackTrace();
				str.append("\t"+trace[0].toString()+"\n");
			}
			return str.toString();
		}
	};
	static Handler console=new Handler() {
		@Override
		public void publish(LogRecord r) {
			String s=getFormatter().format(r);
			if (r.getLevel()==Level.SEVERE||r.getLevel()==Level.WARNING){
				System.err.append(s);
				System.err.flush();
			}
			else {
				System.err.flush();
				System.out.append(s);
				System.out.flush();
			}
		}
		@Override
		public void flush() {
			System.out.flush();
			System.err.flush();
		}
		@Override
		public void close() throws SecurityException {}
	};
	static FileHandler fileHnd=new FileHandler();
	private final LogRecord rec=new LogRecord(Level.OFF,null);
	private int lev=2;
	private final List<Handler> hnd=new ArrayList<Handler>();
	private final StringBuilder buf=new StringBuilder();
	private boolean threadShow=false;
	private boolean buffered=false;
	private Logger(){
		//clear java LoggerManager
		//LogManager.getLogManager().reset();

		//clear java Logger
		java.util.logging.Logger jl=java.util.logging.Logger.getLogger("");
		Handler[] h=jl.getHandlers();
		for (int i=0; i<h.length; ++i) jl.removeHandler(h[i]);
		jl.setFilter(null);

		URL url=this.getClass().getProtectionDomain().getCodeSource().getLocation();
		if (url.toString().endsWith("/")){
			lev=3; dt=dtShort;
			addHandler(console);
		}
		else {
			addFileHandler(url.getFile().replace("jar", "log"));
		}
	}

	synchronized public static Logger getLogger(){
		if (logger==null) logger=new Logger();
		return logger;
	}

	public void setLevel(int l){lev=l;}
	public int getLevel(){return lev;}
	public void setThreadShow(boolean t){threadShow=t;}
	public void setBuffered(boolean b){
		if (buffered!=b) buf.setLength(0);
		buffered=b;
	}
	public boolean getBuffered(){return buffered;}
	synchronized public String getBuffer(){
		String s=buf.toString();
		buf.setLength(0);
		return s;
	}
	public void close(){
		for (int i=0; i<hnd.size(); ++i) hnd.get(i).close();
	}
	synchronized public void addHandler(Handler h){
		if (!hnd.contains(h)) {
			if (h.getFormatter()==null) h.setFormatter(defaultFmt);
			hnd.add(h);
			java.util.logging.Logger.getLogger("").addHandler(h);
			//java.util.logging.Logger.getGlobal().addHandler(h);
		}
	}
	public void addConsole(){
		addHandler(console);
	}
	public void addFileHandler(String fn){
		fileHnd.setFile(fn);
		addHandler(fileHnd);
	}
	public String getFile(){
		return fileHnd.getFile();
	}

	synchronized public void writelog(int l,Object... args){
		if (args.length==0) return ;
		if (l>lev) {
			return ; //filter
		}
		StringBuilder str=new StringBuilder();
		Throwable e=null;
		String fmt=null;
		if (l<0) str.append("[F] ");
		else if (l==0) str.append("[E] ");
		else if (l==1) str.append("[I] ");
		else if (l==2) str.append("[W] ");
		else str.append("[D] ");
		if (threadShow && (fmt=Thread.currentThread().getName())!=null){
			str.append("["+fmt+"] ");
			fmt=null;
		}
		int a0;
		if (l==0 || (l!=1 && lev>2)) {//line numbers only in debug level and not for info
			StackTraceElement trace[]=new Throwable().fillInStackTrace().getStackTrace();
			for (a0=0; a0<trace.length && trace[a0].getClassName().equals(this.getClass().getName()); ++a0) ;
			if (trace[a0].getLineNumber()>=0)
				str.append("("+trace[a0].getFileName()+":"+trace[a0].getLineNumber()+") ");
		}

		a0=0;
		if (args[0] instanceof Throwable){
			e=(Throwable)args[0]; ++a0;
		}
		if (a0<args.length){
			if (args[a0] instanceof String)
				{fmt=(String)args[a0]; ++a0;}
		}

		if (fmt!=null) {
			for (int i=a0;i<args.length;++i) args[i-a0]=args[i];
			for (int i=args.length-a0;i<args.length;++i) args[i]=null;
			if (args.length>a0)
				str.append(String.format((Locale)null,fmt,args));
			else
				str.append(fmt);
			str.append('\n');
		}
		else for (int i=a0;i<args.length;++i) {
			str.append(args[i].toString());
			str.append('\n');
		}
		if (str.length()>0) str.setLength(str.length()-1);
		//System.err.printf("%s %s",log.getLevel().getName(),str.toString());
		//FINER=400,FINE=500,CONFIG=700,INFO=800,WARNIG=900,SEVERE=1000
		Level lv=null;
		if (l<1) lv=Level.SEVERE;
		else if (l==1) lv=Level.INFO;
		else if (l==2) lv=Level.WARNING;
		else lv=Level.FINE;
		rec.setMillis(System.currentTimeMillis());
		rec.setLevel(lv); rec.setMessage(str.toString());
		rec.setThrown(e);
		if (buffered) {buf.append(str);buf.append('\n');}
		for (int i=0; i<hnd.size(); ++i) hnd.get(i).publish(rec);
		for (int i=0; i<hnd.size(); ++i) hnd.get(i).flush();
	}
	final public void fatal(Object... args) { writelog(-1,args); }
	final public void error(Object... args) { writelog(0,args); }
	final public void info(Object... args) { writelog(1,args); }
	final public void warn(Object... args) { writelog(2,args); }
	final public void debug(Object... args) { writelog(3,args); }
	final public void log(int l,Object... args) { writelog(l,args); }

	final static public void Fatal(Object... args) { getLogger().fatal(args); }
	final static public void Error(Object... args) { getLogger().error(args); }

	static class FileHandler extends Handler {
		static final long MINUTE = 1000*60;
		static final long HOUR = 60*MINUTE;
		static final long DAY = 24*HOUR;

		SimpleDateFormat tmfmt=new SimpleDateFormat("yyyy-MM-dd");
		String fnpatt;
		String currfn;
		long fmtCheck=0;
		FileWriter out=null;
		long createTime=(System.currentTimeMillis()/DAY)*DAY+30*MINUTE;

		public String getFile(){
			return this.fnpatt;
		}
		public void setFile(String fn){
			this.fnpatt=fn;
			close();
		}

		private void rotate() {
			String[] rotext={"",".1",".2",".3",".4",".5"};
			close();
			for (int i=0; i+1 < rotext.length; ++i) {
				File src=new File(this.fnpatt+rotext[rotext.length-i-2]);
				File dst=new File(this.fnpatt+rotext[rotext.length-i-1]);
				if (dst.exists()) dst.delete();
				src.renameTo(dst);
			}
			createTime=(System.currentTimeMillis()/DAY)*DAY+30*MINUTE;
		}

		@Override
		public void publish(LogRecord r) {
			if (fnpatt==null) return ;
			if (fmtCheck<System.currentTimeMillis()) {
				fmtCheck=System.currentTimeMillis()+MINUTE;
				if (fnpatt.indexOf("%T")>=0) {
					String fn=StrUtil.replace(fnpatt,
							new String[]{"%T"},
							new String[]{tmfmt.format(new Date())});
					if (!fn.equals(currfn)) {
						close();
						currfn=fn;
					}
				}
				else {
					if (createTime+DAY < System.currentTimeMillis()) {
						close();
						rotate();
					}
					currfn=fnpatt;
				}
			}
			if (out==null){
				try {
					out=new FileWriter(currfn,true);
				} catch (IOException e) {
					e.printStackTrace();
					return ;
				}
			}
			String s=getFormatter().format(r);
			try { if (out!=null) out.write(s); } catch (IOException e) {e.printStackTrace();}
		}
		@Override
		public void flush() {
			if (out!=null) try { out.flush(); } catch (IOException e) {}
		}
		@Override
		public void close() throws SecurityException {
			flush();
			if (out!=null) {
				IOUtils.close(out);
				out=null;
			}
		}
	}

	public static StringBuilder fromLog(String msg){
		StringBuilder buf=new StringBuilder();
		return fromLog(msg,buf);
	}
	public static StringBuilder fromLog(String msg,StringBuilder buf){
		int x;
		boolean hex=true;
		buf.setLength(0);
		for (int i=0; i < msg.length(); i++){
			char c=msg.charAt(i);
			if (c<0x20||c>0xff) continue;
			if (hex && !((c>='0' && c<='9')||(c>='A' && c<='F')||(c>='a' && c<='f'))){
				//getInstance().debug("hex=false, msg[%d]=0x%02X",i,(int)c);
				hex=false;
			}
			buf.append(c);
		}
		if (hex && (buf.length()&1)!=0) buf.append('0');
		msg=buf.toString();
		buf.setLength(0);
		if (hex){
			buf.append(StrUtil.binstr(msg));
			return buf;
		}
		for (int i=0; i < msg.length(); i++){
			char c=msg.charAt(i);
			if (c=='<'){
				x=msg.indexOf('>',i);
				if (x>i && x<=i+3){
					c=(char)Integer.parseInt(msg.substring(i+1,x),16);
					i=msg.indexOf('>',i);
				}
			}
			else if (c=='['){
				x=msg.indexOf(']',i);
				int x2=msg.indexOf('/',i);
				if (x2>0 && x2<x) x=x2;
				if (x>i+1 && x<i+5){
					try{
					c=(char)Integer.parseInt(msg.substring(i+1,x),10);
					i=msg.indexOf(']',i);
					if (c==']') ++i;
					}catch (Exception e) {}
				}
			}
			buf.append(c);
		}
		return buf;
	}

}
