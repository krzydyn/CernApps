package com.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import sys.StrUtil;
import sys.SysUtil;

/**
 *
 * @author KySoft, Krzysztof Dynowski
 *
 */
public class TCPStream extends IOStream {
	//final static String reTCPIP="[^:;]*:[0-9]{1,5}";
	final static String reIPv4="[0-9]{1,3}(\\.[0-9]{1,3}){3}";
	final static String reInt255="(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]|[1-9])";
	final static String reIPv4accurate=reInt255+"(\\."+reInt255+"){3}";

	// RC4
    public final static String SSL_RSA_WITH_RC4_128_SHA = "SSL_RSA_WITH_RC4_128_SHA";

    // 3DES
    public final static String SSL_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";
    public final static String SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA = "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA";
    public final static String SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA = "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA";

    // AES-128
    public final static String TLS_RSA_WITH_AES_128_CBC_SHA = "TLS_RSA_WITH_AES_128_CBC_SHA";
    public final static String TLS_DHE_RSA_WITH_AES_128_CBC_SHA = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA";
    public final static String TLS_DHE_DSS_WITH_AES_128_CBC_SHA = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA";

    // AES-256
    public final static String TLS_RSA_WITH_AES_256_CBC_SHA = "TLS_RSA_WITH_AES_256_CBC_SHA";
    public final static String TLS_DHE_RSA_WITH_AES_256_CBC_SHA = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA";
    public final static String TLS_DHE_DSS_WITH_AES_256_CBC_SHA = "TLS_DHE_DSS_WITH_AES_256_CBC_SHA";

	public final static String[] SSL_PROTOCOLS ={
		"TLSv1", "SSLv3", "SSLv2", "SSLv2Hello"
	};

	public final static String[] SSL_CIPHERS ={
		SSL_RSA_WITH_RC4_128_SHA,
		SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,
		TLS_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
		TLS_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA
	};

	protected String proto = null;
	protected String host = null;
	protected int port = 0;
	protected String userdata=null;

	private SSLSocketFactory sslFactory=null;
	private Socket socket = null; //current socket
	private Socket raw_socket=null,ssl_socket=null;
	private InetAddress addr=null;
	private boolean ssl=false;
	private final String[] ciphers=null;
	private final String[] prots=null;
	//keys and certificates
	private final byte[] ca=null;
	//private byte[] cert=null;
	private final byte[] pk=null;
	private final String pkpass=null;

	//System.setProperty("javax.net.ssl.keyStore", "mykeystore");
    //System.setProperty("javax.net.ssl.keyStorePassword", "wshr.ut");

	public TCPStream(){}
	public TCPStream(String uri){parseURI(uri);}
	public TCPStream(String host,int port) {
		this.host = host;
		this.port = port;
	}
	public TCPStream(Socket s) {
		this.raw_socket=this.socket=s;
		try{
			this.addr=s.getLocalAddress();
			this.host=this.addr.getHostAddress();
			this.port=s.getLocalPort();
			setIO(new SockPollStream(socket),socket.getOutputStream());
			socket.setSoTimeout(chrtmo); //reading timeout in [ms]
			socket.setTcpNoDelay(false);//use Nagle's algorithm
		}catch (Exception e) {}
	}
	/**
	 * @param uri Uniform Resource Identifier
	 * @throws Exception
	 */
	public void parseURI(String uri) {
		if (uri.indexOf(' ')>0) uri=uri.substring(0,uri.indexOf(' '));
		int i=0,r;
		if ((r=uri.indexOf("://",i))>=0) {
			proto=uri.substring(i,r); i=r+3;
		}
		if (proto==null) ;
		else if ("ssl".equals(proto)) {ssl=true;}
		else if ("ssh".equals(proto)) {ssl=true;port=22;}
		else if ("https".equals(proto)) {ssl=true;port=443;}
		else if ("http".equals(proto)) {ssl=false;port=80;}
		if ((r=uri.indexOf("/",i))<0) r=uri.length();
		host=uri.substring(i,r); i=r;
		userdata=uri.substring(i);
		if ((r=host.indexOf(":"))>=0){
			port=Integer.parseInt(host.substring(r+1));
			host=host.substring(0,r);
		}
		if (port==0) throw new RuntimeException("Invalid URL");
		log.debug("host=%s port=%d userdata=%s",host,port,userdata);
	}
	@Override
	public void close(){
		super.close();
		if (raw_socket!=null){
			Socket r=raw_socket,s=ssl_socket;
			socket=null; ssl_socket=null; raw_socket=null;
			IOUtils.close(s);
			IOUtils.close(r);
			log.debug("socket closed %s",toString());
		}
	}
	public String getProtocol() {return proto;}
	public int getPort() {return port;}
	public String getHost() {return host;}
	public String getUserData(){return userdata;}
	public boolean isSSL(){ return socket!=null && socket==ssl_socket; }
	public void startSSL(){
		if (isSSL()) return ;
		try{
			if (ssl_socket==null){
				SSLSocketFactory sf = sslFactory();
				ssl_socket=sf.createSocket(raw_socket,null,port,false);
				sslinit((SSLSocket)ssl_socket);
			}
			((SSLSocket)ssl_socket).startHandshake();
			socket=ssl_socket;
			setIO(socket.getInputStream(),socket.getOutputStream());
			log.debug("Started SSL");
		}catch (Exception e) {log.debug(e);}
	}
	public void stopSSL(){
		if (!isSSL()) return ;
		socket=raw_socket;
		try{
			socket.setSoTimeout(chrtmo); //reading timeout in [ms]
			setIO(socket.getInputStream(),socket.getOutputStream());
			log.debug("Stopped SSL");
		}catch (Exception e) {log.debug(e);}
	}
	@Override
	public void setChrTmo(int t){
		super.setChrTmo(t);
		if (socket==null) return ;
		try{socket.setSoTimeout(t);}catch (Exception e) {log.error(e);}
	}
	@Override
	public String toString(){
		String modetxt[]=new String[]{"8n","7e","7o","7n"};
		String host=this.host;
		if (!host.matches(reIPv4) && addr!=null){
			host+=","+addr.getHostAddress();
		}
		if ((mode&MO_BITS_MSK)==0) return host+":"+port;
		return host+":"+port+" swmod="+modetxt[mode&MO_BITS_MSK];
	}

	protected SSLSocketFactory sslFactory(){
		if (sslFactory!=null) return sslFactory;
		TrustManager[] trustAllCerts = new TrustManager[]{
	        new X509TrustManager() {
	            @Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	            @Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
	            	log.debug("checkClientTrusted certs=%d auth=%s",certs.length,authType);
	            }
	            @Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
	            	log.debug("checkServerTrusted certs=%d auth=%s",certs.length,authType);
	            	//for (int i=0; i<certs.length; ++i)log.debug("cert[%d]: %s",i,certs[i].toString());
	            }
	        }
	    };
		KeyManager[] defKeyMan=null;

		TrustManagerFactory tmf=null;
		if (ca!=null){
			try{
				KeyStore ks=KeyStore.getInstance("JKS");
				CertificateFactory cf = CertificateFactory.getInstance("X.509");{
					Certificate c = cf.generateCertificate(new ByteArrayInputStream(ca));
					ks.setCertificateEntry("CA", c);
				}
				String alg=TrustManagerFactory.getDefaultAlgorithm();//"SunX509"
				tmf = TrustManagerFactory.getInstance(alg);
				tmf.init(ks);
			}catch (Exception e) {
			}
		}
		KeyManagerFactory kmf=null;
		if (pk!=null){
			try{
				KeyStore ks=KeyStore.getInstance("JKS");
				//add pk to keystore
				String alg=KeyManagerFactory.getDefaultAlgorithm();//"SunX509"
				kmf = KeyManagerFactory.getInstance(alg);
				kmf.init(ks,pkpass.toCharArray());
			}catch (Exception e) {
			}
		}
		try {
			KeyManager[] km=null;
			TrustManager[] tm=null;
			if (kmf!=null) km=kmf.getKeyManagers();
			else km=defKeyMan;
			if (tmf!=null) tm=tmf.getTrustManagers();
			else tm=trustAllCerts;
	        SSLContext sc = SSLContext.getInstance("SSLv3");
			//SSLContext sc = SSLContext.getInstance("TLS");
	        //log.debug("km=%s tm=%s",km,tm);
	        sc.init(km,tm,null);
	        sslFactory=sc.getSocketFactory();
	    } catch (Exception e) {
	    	sslFactory=(SSLSocketFactory)SSLSocketFactory.getDefault();
	    }
	    return sslFactory;
	}

	private void sslinit(SSLSocket s){
		if (ciphers!=null) s.setEnabledCipherSuites(ciphers);
		if (prots!=null) s.setEnabledProtocols(prots);
		//s.setEnableSessionCreation(true);
		//s.setNeedClientAuth(false);//for server socket
		//s.setUseClientMode(true);
		//s.setWantClientAuth(true);
		//s.setSSLParameters(null);//set ciphers,protocols and client use/want
	}

	@Override
	public void open() throws IOException {
		// Establish connection to the server
		InetSocketAddress socaddr = new InetSocketAddress(host, port);
		addr=socaddr.getAddress();
		log.info("Connecting %s (tmo=%d ms)",toString(),connTmo);
		raw_socket=SocketFactory.getDefault().createSocket();
		if (raw_socket==null) throw new RuntimeException("can't create socket");
		long tm=SysUtil.timer_get();
		long tmo=SysUtil.timer_start(connTmo);
		for (int n=0;;n++){
			try{
				raw_socket.connect(socaddr,connTmo);
			}
			catch (java.nio.channels.AlreadyConnectedException e) {}
			//catch (java.net.UnknownHostException e){throw e;}
			//catch (java.net.ConnectException e){throw e;}
			catch (IOException e) {
				log.error(e,"%s: %s after %d ms",toString(), e.getMessage(),SysUtil.timer_get()-tm);
				throw e;
			}
			while (!raw_socket.isConnected()){
				log.debug("notConnected after %d ms",SysUtil.timer_get()-tm);
				if (SysUtil.timer_expired(tmo))
					throw new java.net.SocketTimeoutException(toString());
				SysUtil.delay(SysUtil.SECOND/5);
			}
			socket=raw_socket;
			if (ssl) startSSL();
			try {
				//socket.setSendBufferSize(500);
				//setIO(new SockPollStream(socket),socket.getOutputStream());
				setIO(socket.getInputStream(),socket.getOutputStream());
				socket.setTcpNoDelay(false);//use Nagle's algorithm
				socket.setSoTimeout(chrtmo); //reading timeout in [ms]
			}
			catch (IOException e){
				close();
				log.error(e,e.getMessage()+" (try "+n+") after %d ms",SysUtil.timer_get()-tm);
				SysUtil.delay(SysUtil.SECOND/5);
				continue;
			}
			break;
		}
		log.info("Connected "+toString()+" after %d ms",SysUtil.timer_get()-tm);
		//doScript(start);
		connected=true;
	}

    public static boolean verify(X509Certificate cert, PublicKey key)
	    throws CertificateException, InvalidKeyException,
	    NoSuchAlgorithmException, NoSuchProviderException {

	    String sigAlg = cert.getSigAlgName();
	    String keyAlg = key.getAlgorithm();
	    sigAlg = sigAlg != null ? sigAlg.trim().toUpperCase() : "";
	    keyAlg = keyAlg != null ? keyAlg.trim().toUpperCase() : "";
	    if (keyAlg.length() >= 2 && sigAlg.endsWith(keyAlg)) {
	        try {
	            cert.verify(key);
	            return true;
	        } catch (SignatureException se) {
	            return false;
	        }
	    } else {
	        return false;
	    }
    }
    public static InetAddress getLocalAddress(String dest){
    	InetAddress ia=null;
    	try{
       	//InetAddress dst=InetAddress.getByName(dest);
       	//log.debug("dst = %s",dst.getHostAddress());
    	Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();
		for(int i=0; nets.hasMoreElements(); ++i) {
			NetworkInterface ni=nets.nextElement();
			if (!ni.isUp()) continue;
			log.debug("ni[%d]=%s HW=%s '%s' isUp=%B",i,ni.getName(),StrUtil.hex(ni.getHardwareAddress()),ni.getDisplayName(),ni.isUp());
			Enumeration<InetAddress> addrs=ni.getInetAddresses();
			while (addrs.hasMoreElements()) {
				InetAddress a=addrs.nextElement();
				String fqdn=a.getCanonicalHostName();
				log.debug("  addr: %s, %s",a,fqdn);
				if (fqdn.indexOf(".")>0) ia=a;
				a=null;
			}
		}
    	}catch (Exception e) {}
    	return ia;
    }
}
