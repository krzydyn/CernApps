package sys;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Krzysztof Dynowski
 * regex: $[\s]+\{
 * replace: {
*/
public class SysUtil{
	public final static Logger log = Logger.getLogger();
	public final static long tm0=System.currentTimeMillis();

	public final static int SECOND=1000; //one second
	public final static String CRLF="\r\n";

	public final static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().startsWith("mac");
	}

	/**
	 * delay (suspend current thread execution)
	 * @param milis time in miliseconds
	 */
	public static void delay(long dtm) {
		if (dtm>0) try{Thread.sleep(dtm);}
		catch (InterruptedException e) {
			//important to set the flag for current thread
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("sleep %s",e.getClass().getName());
		}
	}
	public static void waitfor(long tm) {
		delay(tm-System.currentTimeMillis());
	}

	/**
	 * make a beep
	 */
	public static void beep() {
		Toolkit.getDefaultToolkit().beep();
	}
	public static void beep(int n) {
		while (n-->0) {
			Toolkit.getDefaultToolkit().beep();
			delay(500);
		}
	}
	/**
	 * Get time from application start
	 * @return current time in miliseconds
	 */
	public static long timer_get() { return System.currentTimeMillis()-tm0; }
	/**
	 * Setup timer
	 * @param n timer distance in miliseconds
	 * @return calculated expire time in miliseconds
	 *
	 */
	public static long timer_start(int n) { return n==0?0:timer_get()+n; }
	/**
	 * Check timer is expired
	 * @param t timer previously setup by {@link timer_get}
	 * @return true if timer is expired
	 * <div>use Misc.SECOND constant with this function</div>
	 */
	public static boolean timer_expired(long t) { return t!=0 && t<timer_get(); }

	public static void or(byte[] b1,int b2,int l){
		for (int i=0; i<l; i++) b1[i]|=b2;
	}
	public static boolean contains(int b,int[] list){
		for (int i=0;i<list.length;i++) if (list[i]==b) return true;
		return false;
	}

	public static Exception rebuildException(Exception e,String msg){
		Exception ne;
		try{
			Constructor<? extends Exception> c=e.getClass().getConstructor(new Class[]{String.class});
			ne=c.newInstance(msg);
		} catch (Exception e1) { ne=e; }
		//StackTraceElement trace[]=new Throwable().fillInStackTrace().getStackTrace();
		ne.setStackTrace(e.getStackTrace());
		return ne;
	}
	public static void deleteFiles(String path,FileFilter which) throws Exception{
		int nd=0;
		File file = new File(path);
		File[] ftd = file.listFiles(which);
		for (int i = 0; i < ftd.length; i++){
			ftd[i].delete();
			if (ftd[i].exists()) nd++;
		}
		if (nd>0) throw new Exception("Can't delete "+nd+"files");
	}
	public static int ctrlNIP(String buf){
		int w[]={6,5,7,2,3,4,5,6,7};
		if (buf.length()!=w.length+1) return -1;
		int s=0;
		for(int i=0; i<buf.length()-1;i++)
			s+=w[i]*(buf.charAt(i)-'0');
		s%=11; s%=10;
		return s;
	}
	public static int ctrlPESEL(String buf){
		int w[]={1,3,7,9,1,3,7,9,1,3};
		if (buf.length()!=w.length+1) return -1;
		int s=0;
		for(int i=0; i<buf.length()-1;i++)
			s+=w[i]*(buf.charAt(i)-'0');
		s%=10; s=10-s; s%=10;
		return s;
	}
	public static boolean contains(Object[] a,Object o){
		if (o==null) return false;
		for (int i=0; i<a.length; ++i)
			if (o.equals(a[i])) return true;
		return false;
	}
	public static void addClassPath(String path,FileFilter which) {
		//URLClassLoader ul=(URLClassLoader)ClassLoader.getSystemClassLoader();
		//ul.addURL - is protected
		Object[] arg=new Object[1];
		File[] files = new File(path).listFiles(which);
		try{
		URL[] urls=((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
	    Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
	    method.setAccessible(true);
		for (int i=0; i<files.length; ++i){
			arg[0]=files[i].toURI().toURL();
			if (contains(urls,arg[0])) continue;
			method.invoke(ClassLoader.getSystemClassLoader(), arg);
			log.info("add %s",arg[0]);
		}
		}catch (Exception e) {
			log.error(e);
		}
	}
	/*
	 * http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	 */
	public static void addLibraryPath(String path) {
		// setup library path
		//The idea is to make the static field "sys_paths" null
		//so that it would construct the paths from the changed value.
		File f=new File(path);
		if (f.exists() && f.isDirectory()){
			String libpath=System.getProperty("java.library.path");
			System.setProperty("java.library.path",libpath+";"+f.getPath());
			//log.debug("set libpath="+System.getProperty("java.library.path"));
			libpath=System.getProperty("LD_LIBRARY_PATH");
			System.setProperty("LD_LIBRARY_PATH",libpath+":"+f.getPath());
		}
		else log.debug("path not exists: "+f.getPath());
		//clear ClassLoader.class.sys_paths
		//this works on Win but not on Linux
		try{
			Class<?> c = ClassLoader.class;
			Field field = c.getDeclaredField("sys_paths");
			field.setAccessible(true);
			field.set(c,null);
		}catch (Exception e) {log.error(e);}
	}

	static public Class<?> loadClass(String cla) throws Exception{
		if (cla==null) throw new NullPointerException();
		Class<?> c=Class.forName(cla);
		log.info("class "+cla+" loaded");
		return c;
	}

	static public void exec(String cmd){
		try {
		    // Execute a command
		    //Process child =
		    	Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
		}
	}

	static public boolean isUnix(){
		return File.separator.equals("/");
	}
    static void rgb2hsb(int rgb, float[] hsb){
    	int r,g,b,mn,mx,d;
		r=(rgb>>16)&0xff;
		g=(rgb>>8)&0xff;
		b=rgb&0xff;
    	mn=Math.min(r,Math.min(g,b));
    	mx=Math.max(r,Math.max(g,b));
    	d=mx-mn;
    	//brightness
    	hsb[2]=mx*(100.0f/255.0f);
    	//saturation
    	if (mx>0){
    		hsb[1]=100.0f*d/mx;
        	if (r==mx) hsb[0]=(g-b)/(float)d;
        	else if (g==mx) hsb[0]=2.0f+(g-b)/(float)d;
        	else if (b==mx) hsb[0]=4.0f+(g-b)/(float)d;
        	else hsb[0]=0;
    	}
    	else {hsb[1]=0; hsb[0]=-1;}
    	hsb[0]*=60;
    	if (hsb[0]<0) hsb[0]+=360;
    }
	public static void put_contents(String fn, String s) {
		Writer o=null;
		try{
		o=new FileWriter(fn);
		o.write(s);
		}
		catch (Exception e) {}
		finally{if (o!=null)try{o.close();}catch (Exception e2){}}
	}
	public static boolean get_contents(String fn,StringBuilder b) {
		byte[] tmp=new byte[256];
		FileInputStream in=null;
		try{
		int r;
		in=new FileInputStream(fn);
		while ((r=in.read(tmp))>0)
			for (int i=0; i<r; ++i) b.append((char)tmp[i]);
		}
		catch (Exception e) {return false;}
		finally{if (in!=null)try{in.close();}catch (Exception e){}}
		return true;
	}
	public static String get_contents(String fn) {
		StringBuilder b=new StringBuilder();
		if (!get_contents(fn, b)) return null;
		return b.toString();
	}
	public static int getInt(byte[] data,int offs,int len) {
		int r=0;
		for (int i=0; i<len; ++i) {r<<=8; r|=data[offs+i]&0xff;}
		return r;
	}
	public static int getInt(byte[] data, int len) {
		return getInt(data, 0, len);
	}
	public static void putInt(int v,byte[] data,int offs,int len) {
		for (int i=0; i<len; ++i) { data[offs+len-i-1]=(byte)(v&0xff); v>>=8;}
	}
	public static void putInt(int v,byte[] data,int len) {
		putInt(v, data, 0, len);
	}

}
