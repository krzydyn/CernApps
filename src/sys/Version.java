package sys;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import com.io.IOUtils;
import com.io.TCPStream;

public class Version {
	final public Class<?> main;
	final public String basename;
	final public String version;
	final public String buildno;
	final public String buildtime;
	final public String location;
	final public boolean DEBUG;
	final public static Logger log=Logger.getLogger();
	private static Version instance=null;

	public static void createInstance(Class<?> main)
	{
		if (instance==null) instance=new Version(main);
		else throw new RuntimeException("one Version instance allowed");
	}
	public static Version getInstance() {return instance;}

	private Version(Class<?> main)
	{
		String[] v;
		String bt;
		this.main=main;
		String loc=null;
		boolean dbg=true;
		basename=main.getSimpleName().toLowerCase();
		//Logger.getLogger().setFile(basename+".log");
		if (log.getFile()==null)
			log.addFileHandler(basename+".log");
		/*
		java.util.Properties p=System.getProperties();
		for (java.util.Enumeration<?> i=p.keys(); i.hasMoreElements(); )
		{
			Object key=i.nextElement();
			log.info("%s = %s",key,p.get(key));
		}*/
		String jvm=System.getProperty("java.vm.name");
		if (jvm.indexOf("gcj")>=0)
		{
			log.error("\n"+
					"********************************************************************\n"+
					"This application does not work with GCJ jvm. Please install SUN jvm.\n"+
					"********************************************************************\n");
			throw new RuntimeException("This application does not work with GCJ jvm. Please install SUN jvm.");
		}

		v="0".split("-");bt="";
		try{
			if (loc==null)
			{
				//can throw SecurityException
				URL url=main.getProtectionDomain().getCodeSource().getLocation();
				loc=StrUtil.toString(url);
				//if (loc.endsWith("/")) loc=loc.substring(0,loc.length()-1);
			}
			//log.debug("url="+url);
			JarFile jar;
			if (loc.endsWith(".jar")) {dbg=false;jar=new JarFile(loc);}
			else jar=new JarFile(main.getSimpleName().toLowerCase()+".jar");
			Attributes attr=jar.getManifest().getMainAttributes();
			v=attr.getValue("Implementation-Version").split("-");
			bt=attr.getValue("Built-Date");
			jar.close();
		}
		catch (Exception e) {}
		try{
			Field field = main.getDeclaredField("DEBUG");
			field.setAccessible(true);
			dbg=(Boolean)field.get(main);
		}catch (Exception e) {}
		//if (Logger.getPath()==null)
		// Logger.setPath(loc.substring(0,loc.lastIndexOf('/')+1));
		log.debug("Creating Version object for %s",main.getCanonicalName());
		location=loc;
		version=v[0];
		buildno=v.length>1?v[1]:"0";
		buildtime=bt;
		DEBUG=dbg;
		if (DEBUG) log.setLevel(3);
		log.info("Version %s, DEBUG is "+DEBUG,toString());
		log.debug("Main class location is '%s'",location);

		String dirjni=location.substring(0,location.lastIndexOf('/'));
		if (dirjni==null) dirjni=".";

		if (location.endsWith(".jar")){
			IOUtils.unzip(location,dirjni,new FileFilter(){
				@Override
				public boolean accept(File pathname) {
					return pathname.getPath().indexOf("jni")==0;
				}
			});
		}
		else dirjni=new File(dirjni).getParent();
		if (dirjni.charAt(dirjni.length()-1)!=File.separatorChar)
			dirjni+=File.separatorChar;
		dirjni+="jni";
		SysUtil.addLibraryPath(dirjni);
	}
	@Override
	public String toString()
	{
		return "v"+version+" (build "+buildno+")";
	}
	public static String getVersion(String uri) throws IOException{
		Version v=Version.getInstance();
		if (v==null) return null;
		TCPStream c=new TCPStream(uri);
		String get=c.getUserData();
		if (get==null) get="/";
		try{
		c.setRxTmo(10*SysUtil.SECOND);
		c.setChrTmo(SysUtil.SECOND);
		c.open();
		//TODO use HttpURLConnection
		StringBuilder b=new StringBuilder("GET ");
		b.append(get); b.append("/version.php?");
		b.append("f=");b.append(v.basename);b.append(".jar");
		b.append(" HTTP/1.0\r\n");
		b.append("Host:"); b.append(c.getHost()); b.append("\r\n");
		b.append("\r\n");
		log.info("version request: %s",b.toString());
		c.write(b);
		String type="";
		do {
			b.setLength(0);
			if (c.readln(b)<0) break;
			int x;
			if ((x=b.indexOf("Content-Type:"))>=0) type=b.substring(x+13).trim();
		}while (b.length()>2||type.isEmpty());
		if (type.indexOf("text/html")<0) throw new IOException("Wrong version response");
		c.readUntil(b, -1);
		String ver=b.toString().trim();
		log.info("version on server: %s",StrUtil.vis(ver));
		return ver;
		}
		finally{ if (c!=null) c.close(); }
	}
	public static boolean getDownload(String uri)throws IOException{
		Version v=Version.getInstance();
		if (v==null) return false;

		TCPStream c=new TCPStream(uri);
		String get=c.getUserData();
		if (get==null) get="/";
		File tmp=null;
		OutputStream out=null;
		try{
		c.setRxTmo(10*SysUtil.SECOND);
		c.setChrTmo(5*SysUtil.SECOND);
		c.close();
		StringBuilder b=new StringBuilder("GET ");
		b.append(get); if (b.charAt(b.length()-1)!='/') b.append("/");
		b.append("download.php?");
		b.append("f=");b.append(v.basename);b.append(".jar");
		b.append(" HTTP/1.0\r\n");
		b.append("Host:"); b.append(c.getHost()); b.append("\r\n");
		b.append("\r\n");
		c.write(b);
		int i=0;
		String type="";
		int len=0;
		do {
			b.setLength(0);
			if (c.readln(b)<0) break;
			++i;
			int x;
			if ((x=b.indexOf("Content-Type:"))>=0) type=b.substring(x+13).trim();
			else if ((x=b.indexOf("Content-Length:"))>=0) len=Integer.parseInt(b.substring(x+15).trim());
		}while (b.length()>2||type.isEmpty());
		if (type.indexOf("application/java-archive")<0) throw new IOException("Wrong version response");
		log.info("%s.length=%d",v.basename,len);
		tmp=File.createTempFile(v.basename,"-part.jar");
		out=new FileOutputStream(tmp.getAbsolutePath());
		i=0;
		do {
			b.setLength(0);
			int r;
			if ((r=c.read(b,1000))<0) {
				log.error("read(b,1000)=%d",r);
				break;
			}
			if (b.length()==0) break;
			out.write(StrUtil.bytes(b));
			i+=b.length();
		}while (b.length()==1000);
		out.close();out=null;
		if (len>0 && i!=len){
			log.info("error len: %d!=%d",i,len);
			return false;
		}
		if (v.location.endsWith(".jar")){
			IOUtils.copy(v.location,v.location+"~");
			IOUtils.copy(tmp.getAbsolutePath(),v.location);
		}
		else {
			IOUtils.copy(tmp.getAbsolutePath(),v.basename+"-1.jar");
		}
		return true;
		}
		finally{
			if (c!=null) c.close();
			if (out!=null) IOUtils.close(out);
			if (tmp!=null) tmp.delete();
		}
	}
}
