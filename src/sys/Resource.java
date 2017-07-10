package sys;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import com.io.IOUtils;

public class Resource {
	final static Logger log=Logger.getLogger();
	private static HashMap<String,Resource> cached=new HashMap<String,Resource>();

	private final ResourceBundle loaded;
	private Resource(ResourceBundle loaded) {this.loaded=loaded;}
	public String getString(String key){
		try{return loaded.getString(key);}catch (Exception e) {}
		return null;
	}
	public String getString(String key,String def) {
		String s=null;
		try{s=loaded.getString(key);}catch (Exception e) {}
		return s==null?def:s;
	}

	public static URL getResourceURL(String name){
		URL url;
		try{
			if (name==null) url=null;
			else if (name.indexOf("://")<0) url=Resource.class.getResource("/"+name);
			else url=new URL(name);
			if (url==null && new File(name).exists()) url=new URL("file:"+name);
		}catch (Exception e) {url=null;}
		return url;
	}
	public ResourceBundle getBundle() {return loaded;}
	public static Resource getBundle(String name){
		return getBundle(name, Locale.getDefault());
	}
	public static Resource getBundle(String name,Locale loc){
		String cachename=name+"_"+loc.toString();
		Resource res=cached.get(cachename);
		if (res!=null) return res;
		ResourceBundle rb=null;
		try {
			rb=ResourceBundle.getBundle(name,loc);
			//log.debug("Loaded bundle: "+name+", locale="+loc.getClass().getName());
		}catch (Exception e){
			log.debug("no boundle for %s (%s)",name,loc);
		}
		if (rb==null){
			final Properties p=new Properties();
			String fn[]=new String[]{
					name+"_"+loc.getLanguage()+"_"+loc.getCountry(),
					name+"_"+loc.getLanguage(),
					name,
			};
			StringBuffer buf=new StringBuffer();
			for (int i=0; i<fn.length; i++){
				FileInputStream io=null;
				try{
					buf.setLength(0);
					buf.append(fn[i]);buf.append(".properties");
					p.clear();
					p.load(io=new FileInputStream(buf.toString()));
					IOUtils.close(io);
					rb=new ResourceBundle(){
						@Override
						@SuppressWarnings("unchecked")
						public Enumeration<String> getKeys() {
							return (Enumeration<String>)p.propertyNames();
						}
						@Override
						protected Object handleGetObject(String key) {
							return p.getProperty(key);
						}
					};
					//log.debug("loaded props for %s (%s)",name,loc);
					break;
				}
				catch (Exception e) {}
				finally {IOUtils.close(io);}
			}
		}
		//if (rb==null) return null;
		res=new Resource(rb);
		cached.put(cachename,res);
		return res;
	}
}
