package com.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

import sys.Logger;

public class IOUtils {
	protected static Logger log = Logger.getLogger();

	//close operation without exception
	public static void close(Closeable r) {
		if (r==null) return;
		try{r.close();}catch (Throwable e) {log.error(e);}
	}
	//keep for older jvm compatibility
	public static void close(Socket r) {
		if (r==null) return;
		try{r.close();}catch (Throwable e) {log.error(e);}
	}

	public static void close(URLConnection r){
		if (r==null) return ;
		try{
			if (r instanceof HttpURLConnection)
				((HttpURLConnection)r).disconnect();
			else if (r instanceof HttpsURLConnection)
				((HttpsURLConnection)r).disconnect();
		}catch (Throwable e) {}
		try{
			r.getInputStream().close();
		}catch (Throwable e) {}
		try{
			r.getOutputStream().close();
		}catch (Throwable e) {}
	}

	public static void copy(final InputStream is, final OutputStream os) throws IOException {
		final byte[] buffer = new byte[1024];
		for (;;) {
			int r = is.read(buffer, 0, buffer.length);
			if (r < 0) break;
			os.write(buffer, 0, r);
		}
	}

	public static void copyAndClose(final InputStream is, final OutputStream os) throws IOException {
	    try {
		    copy(is, os);
	    }
	    finally {
	    	close(os); close(is);
	    }
	}

	public static String toString(final URL url, final String encoding) throws Exception {
		URLConnection c=null;
		try {
	        c = url.openConnection();
	        return toString(c.getInputStream(), encoding);
		}finally {
			close(c);
		}
	}
	public static String toString(final InputStream is, final String encoding) {
	  final char[] buffer = new char[4096];
	  final StringBuilder out = new StringBuilder();
	  try {
	    final Reader in = new InputStreamReader(is,encoding);
	    try {
	      for (;;) {
	        int rsz = in.read(buffer, 0, buffer.length);
	        if (rsz < 0) break;
	        if (rsz==0) {log.debug("not ready");continue;}
	        out.append(buffer, 0, rsz);
	      }
	    }
	    finally {
	      in.close();
	    }
	  }
	  catch (UnsupportedEncodingException ex) {
		  log.error(ex);
	  }
	  catch (IOException ex) {
	     log.error(ex);
	  }
	  return out.toString();
	}

	public static int unzip(String fnz,String dir,FileFilter which){
		try{
		ZipFile z=new ZipFile(fnz);
		Enumeration<? extends ZipEntry> entries=z.entries();
		File out=new File(dir);
		out.mkdirs();
		while (entries.hasMoreElements()){
			ZipEntry ze=entries.nextElement();
			//Misc.logstr("zipentry %s: method=%d crc=%x\n",
			//		new Object[]{ze.getName(),new Integer(ze.getMethod()),new Integer((int)ze.getCrc())});
			out=new File(ze.getName());
			if (which!=null && !which.accept(out)) continue;
			out=new File(dir+"/"+ze.getName());
			if (ze.isDirectory()) out.mkdirs();
			else{
				if (out.exists()) out.delete();
				copyAndClose(z.getInputStream(ze),new FileOutputStream(out));
			}
		}
		z.close();
		}catch (Exception e) {log.error(e,"unzip");return -1;}
		return 0;
	}
	public static int zip(String fnz,String fn[]){
		byte[] buf=new byte[100];
		try{
			CRC32 crc=new CRC32();
			new File(fnz).delete();
			ZipOutputStream z=new ZipOutputStream(new FileOutputStream(fnz));
			z.setMethod(8);
			z.setLevel(7);
			for (int i=0; i<fn.length; i++){
				File f=new File(fn[i]);
				if (!f.exists()) continue;
				if (f.isDirectory()){
					// make sure it ends with "/"
					if (!fn[i].endsWith("/")) fn[i]+="/";
				}
				ZipEntry ze=new ZipEntry(fn[i]);
				z.putNextEntry(ze);
				crc.reset();
				if (!f.isDirectory()){
					FileInputStream is=new FileInputStream(fn[i]);
					long s=0;
					int n;
					while ((n=is.read(buf))!=-1){
						crc.update(buf,0,n);
						z.write(buf,0,n);s+=n;
					}
					ze.setCrc(crc.getValue());
					ze.setSize(s);
					is.close();
				}
				z.closeEntry();
				log.debug("zipentry %s: method=%d crc=%x",ze.getName(),ze.getMethod(),(int)ze.getCrc());
			}
			z.flush();
			z.close();
		}catch (Exception e) {log.error(e,"zip");new File(fnz).delete();return -1;}
		return 0;
	}
	public static int zip(String fnz,String fn){
		return zip(fnz,new String[]{fn});
	}
	public static void unlink(String fn){
		File f=new File(fn);
		f.delete();
		if (f.exists()) log.error("Can't delete file "+fn);
	}
	public static void rename(String fn,String newfn){
		File f=new File(fn);
		f.renameTo(new File(newfn));
		if (f.exists()) log.error("Can't rename file "+fn+" to "+newfn);
	}
	public static void copy(String fn,String newfn){
		File f=new File(fn);
		if (!f.isFile()) return ;
		try{
			copyAndClose(new FileInputStream(fn),new FileOutputStream(newfn));
		}catch (Exception e) {log.error("Can't copy file "+fn+" to "+newfn);}
	}

}
