package utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import algebra.Matrix;
import sys.Const;
import sys.Logger;

public class ImageOps {
	static Logger log=Logger.getLogger();
	public static BufferedImage getScaledImage(Image srcImg, int w, int h) {
	    BufferedImage resizedImg = null;
	    /*int w0,h0;
	    w0=srcImg.getWidth(null);
	    h0=srcImg.getHeight(null);
	    if (w*2<w0 && h*2<h0){
	    	while (w*2<w0 && h*2<h0) {w0/=2; h0/=2;}
	    	resizedImg = new BufferedImage(w0,h0,BufferedImage.TYPE_INT_RGB);
	    	Graphics2D g2 = resizedImg.createGraphics();
	    	g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	        g2.drawImage(srcImg, 0, 0, w0, h0, null);
	        g2.dispose();
	        srcImg=resizedImg;
	    }*/
	    try {
	        resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	        Graphics2D g2 = resizedImg.createGraphics();
	        //RenderingHints hints = g2.getRenderingHints();
	        //hints.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
	        //hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	        //hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	        //hints.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	        //hints.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	        //hints.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
	        //g2.setRenderingHints(hints);
	        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	        g2.drawImage(srcImg, 0, 0, w, h, null);
	        g2.dispose();
	    } catch (Exception ex) { }
	    return resizedImg;
	}
	public static BufferedImage getScaledImage(Image srcImg, int w, int h, int dstType) {
	    BufferedImage resizedImg = null;
	    try {
	    	if (dstType==0) dstType=BufferedImage.TYPE_INT_RGB;
	        resizedImg = new BufferedImage(w, h, dstType);
	        Graphics2D g2 = resizedImg.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	        g2.drawImage(srcImg, 0, 0, w, h, null);
	        g2.dispose();
	    } catch (Exception ex) {log.error(ex);}
	    return resizedImg;
	}
	public static void getRGB(float[] sum,BufferedImage img,float x,float y){
		int xi=(int)x;
		int yi=(int)y;
		for (int i=0; i<3; ++i) sum[i]=0;
		if (Math.abs(xi-x)<0.1f && Math.abs(yi-y)<0.1f){
			addRGB(sum,img.getRGB(xi,yi),1);
			return ;
		}
		int xm=img.getWidth();
		int ym=img.getHeight();
		int[] c=new int[4];
		c[0]=img.getRGB(xi,yi);
		c[1]=img.getRGB(xi+1<xm?xi+1:xi,yi);
		c[2]=img.getRGB(xi+1<xm?xi+1:xi,yi+1<ym?yi+1:yi);
		c[3]=img.getRGB(xi,yi+1<ym?yi+1:yi);
		float[] w=new float[4];
		w[0]=(xi-x)*(xi-x)+(yi-y)*(yi-y);
		w[1]=(xi+1-x)*(xi+1-x)+(yi-y)*(yi-y);
		w[2]=(xi+1-x)*(xi+1-x)+(yi+1-y)*(yi+1-y);
		w[3]=(xi-x)*(xi-x)+(yi+1-y)*(yi+1-y);
		for (int i=0; i<4; ++i) w[i]=1/(1+8*w[i]*w[i]);
		float ws=0;
		for (int i=0; i<4; ++i){
			addRGB(sum,c[i],w[i]);
			ws+=w[i];
		}
		for (int i=0; i<3; ++i) sum[i]/=ws;
	}
	public static BufferedImage getScaledImageQ(BufferedImage img, int w, int h) {
		BufferedImage rimg;
		if (img.getType()==BufferedImage.TYPE_INT_ARGB || img.getType()==BufferedImage.TYPE_4BYTE_ABGR)
			rimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		else rimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		float dx=img.getWidth(); dx/=w;
		float dy=img.getHeight(); dy/=h;
		//log.debug("dx=%f, dy=%f",dx,dy);
		float[] sum=new float[3];
		if (dx<2.01f || dy<2.01f) {
			//4 near neighbors sampling
			for (int j=0; j<h; ++j)
				for (int i=0; i<w; ++i){
					getRGB(sum,img,i*dx,j*dy);
					if (rimg.getType()==BufferedImage.TYPE_INT_ARGB) {
						int alpha=img.getRGB((int)(i*dx),(int)(j*dy))&0xff000000;
						rimg.setRGB(i,j,rgb(sum)|alpha);
					}
					else rimg.setRGB(i,j,rgb(sum));
				}
		}
		else{
			//area sampling
			//float dx2=dx*dx/4, dy2=dy*dy/4;
			int dw=(int)(dx/2);
			int dh=(int)(dy/2);
			for (int j=0; j<h; ++j)
				for (int i=0; i<w; ++i){
					int xi=(int)(i*dx+0.5f);
					int yi=(int)(j*dy+0.5f);
					float ws=0;
					for (int ii=0; ii<3; ++ii) sum[ii]=0;
					for (int dj=-dh; dj<=dh; ++dj)
						for (int di=-dw; di<=dw; ++di){
							if (xi+di<0||yi+dj<0||xi+di>=img.getWidth()||yi+dj>=img.getHeight())
								continue;
							float f=1;//gives better result
							//float f=di*di/dx2+dj*dj/dy2;
							//f=1/(1+f);
							addRGB(sum,img.getRGB(xi+di,yi+dj),f);
							ws+=f;
						}
					for (int ii=0; ii<3; ++ii) sum[ii]/=ws;
					if (rimg.getType()==BufferedImage.TYPE_INT_ARGB) {
						int alpha=img.getRGB((int)(i*dx),(int)(j*dy))&0xff000000;
						rimg.setRGB(i,j,rgb(sum)|alpha);
					}
					else rimg.setRGB(i,j,rgb(sum));
				}
		}
		return rimg;
	}
	public static void correctGamma(BufferedImage img,float gamma){
		int w=img.getWidth();
		int h=img.getHeight();
		float[] rgb=new float[3];
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i){
				rgbGet(rgb,img.getRGB(i,j));
				for (int x=0; x<3; ++x) rgb[x]=gray2gamma(rgb[x],gamma);
				img.setRGB(i,j,rgb(rgb));
			}
	}
	public static void correctRGB(BufferedImage img,float gamma){
		int w=img.getWidth();
		int h=img.getHeight();
		float[] rgb=new float[3];
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i){
				rgbGet(rgb,img.getRGB(i,j));
				for (int x=0; x<3; ++x) rgb[x]=gamma2gray(rgb[x],gamma);
				img.setRGB(i,j,rgb(rgb));
			}
	}
	public static void makeGray(BufferedImage img) {
		int w=img.getWidth();
		int h=img.getHeight();
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i){
				int c=img.getRGB(i,j);
				float f=rgb2lum(c);
				img.setRGB(i,j,gray2rgb(f)|(c&0xff000000));
			}
	}
	public static BufferedImage imgMult(BufferedImage img,float[] mrgb){
		int w=img.getWidth();
		int h=img.getHeight();
		float[] rgb=new float[3];
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i){
				int c=img.getRGB(i,j);
				//if ((c&0xff000000)!=0) continue;
				rgbGet(rgb,c);
				for (int x=0; x<3; ++x) rgb[x]=mrgb[x]*(rgb[x]+0.1f)-0.1f;
				img.setRGB(i,j,rgb(rgb)|(c&0xff000000));
			}
		return img;
	}
	public static final int
		ALGGREY_AVER=0,
		ALGGREY_LUMI=1,
		ALGGREY_BT709=2;
	public static float rgb2gray(int rgb,int alg) {
		int r,g,b;
		r=(rgb>>16)&0xff;
		g=(rgb>>8)&0xff;
		b=rgb&0xff;
		if (alg==ALGGREY_AVER) return (r+g+b)/3f/255f;
		if (alg==ALGGREY_LUMI) return (0.299f*r+0.587f*g+0.114f*b)/255f;
		if (alg==ALGGREY_BT709) return (0.2125f*r+0.7154f*g+0.0721f*b)/255f;
		return (r+g+b)/3f/255f;
	}
	public static float rgb2lum(int rgb) {
		return rgb2gray(rgb, ALGGREY_LUMI);
	}
	public static float rgb2gray(int rgb) {
		return rgb2gray(rgb, ALGGREY_AVER);
	}
	public static int gray2rgb(float gray) {
		if (gray<0f) gray=0f; else if (gray>1f) gray=1f;
		int c=255; c*=gray;
		c|=c<<8;
		c|=c<<8;
		return c;
	}
	public static float gray2gamma(float v,float g) {
		return (float)Math.pow(v, 1f/g);
	}
	public static float gamma2gray(float v,float g) {
		return (float)Math.pow(v, g);
	}
	public static float gray2sgamma(float v,float g) {
		float a = 0.055f;
		return v<0.04045f?v/12.92f:(float)Math.pow((v+a)/(1+a),g);
	}
	public static float sgamma2gray(float v,float g) {
		float a = 0.055f;
		return v<=0.0031308f?12.92f*v:(1+a)*(float)Math.pow(v,1/g)-a;
	}

	/**
	 * <b>BW matrix</b><br>
	 */
	public static final Matrix BW=new Matrix(0,0);
	/**
	 * <b>Floyd-Steinberg matrix</b><br>
	 * | - x 7|<br>
	 * | 3 5 1| / 16<br>
	 */
	public static final Matrix FloydSteinberg=new Matrix(3,2);
	static {
		FloydSteinberg.set(2, 0, 7.0/16.0);
		FloydSteinberg.set(0, 1, 3.0/16.0);
		FloydSteinberg.set(1, 1, 5.0/16.0);
		FloydSteinberg.set(2, 1, 1.0/16.0);
	}
	/**
	 * <b>Krzydyn matrix</b><br>
	 * | - x 8 4|<br>
	 * | 4 8 4 2| / 40<br>
	 * | 2 4 2 0|<br>
	 */
	public static final Matrix Krzydyn=new Matrix(4,3);
	static {
		Krzydyn.set(2, 0, 8.0);
		Krzydyn.set(3, 0, 4.0);
		Krzydyn.set(0, 1, 4.0);
		Krzydyn.set(1, 1, 8.0);
		Krzydyn.set(2, 1, 4.0);
		Krzydyn.set(3, 1, 2.0);
		Krzydyn.set(0, 2, 2.0);
		Krzydyn.set(1, 2, 4.0);
		Krzydyn.set(2, 2, 2.0);
		Krzydyn.set(3, 2, 0.0);
		double s=0;
		for (int j=0; j<Krzydyn.getHeight(); ++j)
			for (int i=0; i<Krzydyn.getWidth(); ++i)
				s+=Krzydyn.get(i, j);
		s+=2;
		//log.debug("Krzydyn.sum=%f",s);
		for (int j=0; j<Krzydyn.getHeight(); ++j)
			for (int i=0; i<Krzydyn.getWidth(); ++i)
				Krzydyn.set(i, j, Krzydyn.get(i, j)/s);
	}
	/**
	 * <b>Stucki matrix</b><br>
	 * |- - x 8 4|<br>
	 * |2 4 8 4 2| / 42<br>
	 * |1 2 4 2 1|<br>
	 */
	public static final Matrix Stucki=new Matrix(5,3);
	static {
		Stucki.set(3, 0, 8);
		Stucki.set(4, 0, 4);
		Stucki.set(0, 1, 2);
		Stucki.set(1, 1, 4);
		Stucki.set(2, 1, 8);
		Stucki.set(3, 1, 4);
		Stucki.set(4, 1, 2);
		Stucki.set(0, 2, 1);
		Stucki.set(1, 2, 2);
		Stucki.set(2, 2, 4);
		Stucki.set(3, 2, 2);
		Stucki.set(4, 2, 1);
		Stucki.mul(1/42.0);
	}
	/**
	 * <b>Atkinson matrix</b><br>
	 * <table>
	 * <tr><td>|-<td>x<td>1<td>1|</tr>
	 * <tr><td>|1<td>1<td>1<td>0|<td> / 16</tr>
	 * <tr><td>|0<td>1<td>0<td>0|</tr>
	 * </table><br><br>
	 */
	public static final Matrix Atkinson=new Matrix(4,3);
	static {
		Atkinson.set(2, 0, 1);
		Atkinson.set(3, 0, 1);
		Atkinson.set(0, 1, 1);
		Atkinson.set(1, 1, 1);
		Atkinson.set(2, 1, 1);
		Atkinson.set(1, 2, 1);
		Atkinson.mul(1/8.0);
	}
	/**
	 * <b>Shiau-Fan(4 cell) matrix</b><br>
	 * | - - x 1|<br>
	 * | 1 1 2 0| / 8<br>
	 */
	public static final Matrix ShiauFan4=new Matrix(4,2);
	static {
		ShiauFan4.set(3, 0, 1.0/8.0);
		ShiauFan4.set(0, 1, 1.0/8.0);
		ShiauFan4.set(1, 1, 1.0/8.0);
		ShiauFan4.set(2, 1, 2.0/8.0);
	}
	/**
	 * <b>Shiau-Fan(5 cell) matrix</b><br>
	 * | - - - x 8|<br>
	 * | 1 1 2 4 0| / 16<br>
	 */
	public static final Matrix ShiauFan5=new Matrix(5,2);
	static {
		ShiauFan5.set(4, 0, 8.0/16.0);
		ShiauFan5.set(0, 1, 1.0/16.0);
		ShiauFan5.set(1, 1, 1.0/16.0);
		ShiauFan5.set(2, 1, 2.0/16.0);
		ShiauFan5.set(3, 1, 4.0/16.0);
	}

	public static float grayError(float a,float b){
		return Math.abs(a-b);
	}
	public static void rgbError(int c1,int c2,int[] err){
		 err[0]=((c1>>16)&0xff)-((c2>>16)&0xff);
		 err[1]=((c1>>8)&0xff)-((c2>>8)&0xff);
		 err[2]=(c1&0xff)-(c2&0xff);
	}
	public static int rgbMul(int c1,float d){
		int c;
		double r;
		if (c1==0) return 0;
		r=((c1>>16)&0xff)*d;
		if (r<0) r=0; else if (r>255) r=255;
		c=(int)r;
		r=((c1>>8)&0xff)*d;
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=(int)r;
		r=(c1&0xff)*d;
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=(int)r;
		return c;
	}
	public static int rgbAdd(int c1,int c2){
		int c,r;
		if (c1==0) return c2;
		if (c2==0) return c1;
		r=((c1>>16)&0xff)+((c2>>16)&0xff);
		if (r<0) r=0; else if (r>255) r=255;
		c=r;
		r=((c1>>8)&0xff)+((c2>>8)&0xff);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		r=(c1&0xff)+(c2&0xff);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		return c;
	}
	public static int rgb(float[] s){
		int c,r;
		r=(int)(s[0]*255);
		if (r<0) r=0; else if (r>255) r=255;
		c=r;
		r=(int)(s[1]*255);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		r=(int)(s[2]*255);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		return c;
	}
	public static void rgbGet(float[] sum,int c){
		sum[0]=((c>>16)&0xff)/255f;
		sum[1]=((c>>8)&0xff)/255f;
		sum[2]=(c&0xff)/255f;
	}
	public static void rgbAdd(float[] sum,int c){
		sum[0]+=((c>>16)&0xff)/255f;
		sum[1]+=((c>>8)&0xff)/255f;
		sum[2]+=(c&0xff)/255f;
	}
	public static void addRGB(float[] sum,int c,float m){
		sum[0]+=((c>>16)&0xff)/255f*m;
		sum[1]+=((c>>8)&0xff)/255f*m;
		sum[2]+=(c&0xff)/255f*m;

	}
	public static int rgb(double[] s){
		int c,r;
		r=(int)(s[0]*255+0.5);
		if (r<0) r=0; else if (r>255) r=255;
		c=r;
		r=(int)(s[1]*255+0.5);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		r=(int)(s[2]*255+0.5);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		return c;
	}
	public static void rgbAdd(double[] sum,int c){
		sum[0]+=((c>>16)&0xff)/255.0;
		sum[1]+=((c>>8)&0xff)/255.0;
		sum[2]+=(c&0xff)/255.0;

	}
	public static void rgbAdd(double[] sum,int c,double m){
		sum[0]+=((c>>16)&0xff)/255.0*m;
		sum[1]+=((c>>8)&0xff)/255.0*m;
		sum[2]+=(c&0xff)/255.0*m;

	}
	// return c1+err[r,g,b]*d
	public static int rgbAddError(int c1,int[] err,float d){
		int c,r;
		r=(int)(((c1>>16)&0xff)+err[0]*d);
		if (r<0) r=0; else if (r>255) r=255;
		c=r;
		r=(int)(((c1>>8)&0xff)+err[1]*d);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		r=(int)((c1&0xff)+err[2]*d);
		if (r<0) r=0; else if (r>255) r=255;
		c<<=8; c|=r;
		return c;
	}
	public static int lookupColor(int c,int[] colors){
		int[] err=new int[3];
		int c1=c,c2=0,e,e2=3*256;
		for (int i=0; i<colors.length; ++i){
			c=colors[i];
			rgbError(c1,c,err);
			e=Math.abs(err[0])+Math.abs(err[1])+Math.abs(err[2]);
			if (e<e2) {e2=e;c2=c;}
		}
		return c2;
	}
	public static int lookupColorGray(int c,int[] colors) {
		int e,err=255,lc=c&0xff,c2=0;
		for (int i=0; i<colors.length; ++i) {
			c=colors[i]&0xff;
			e=Math.abs(lc-c);
			if (e<err) {err=e;c2=c;}
		}
		return (c2<<16)|(c2<<8)|c2;
	}
	public static void dithering(BufferedImage img,Matrix kern,int[] colors) {
		int x0=0;
		int[] err=new int[3];
		while (x0+1<kern.getWidth() && Math.abs(kern.get(x0+1,0))<Const.eps) ++x0;
		log.debug("dithering on x0=%d:\n%s",x0,kern.toString());
		for (int y=0; y<img.getHeight(); ++y)
			for (int x=0; x<img.getWidth(); ++x) {
				int c=img.getRGB(x, y)&0xffffff;
				int c2=lookupColor(c,colors)&0xffffff;
				// calc error
				rgbError(c,c2,err);
				// diffuse error to neighbors
				for (int my=0; my<kern.getHeight(); ++my) {
					int py=y+my;
					if (py<0||py>=img.getHeight()) continue;
					for (int mx=0; mx<kern.getWidth(); ++mx) {
						int px=-x0+x+mx;
						if (px<0||px>=img.getWidth()) continue;
						float d=(float)kern.get(mx, my);
						if (Math.abs(d)<Const.eps) continue;
						img.setRGB(px,py,rgbAddError(img.getRGB(px,py),err,d));
					}
				}
				img.setRGB(x, y, c2);
			}
	}

	public static void convolvexy(double[] sum,BufferedImage src,int x,int y,Matrix kern) {
		int dy=-kern.getHeight()/2;
		int dx=-kern.getWidth()/2;
		//double[] sum=new double[3];
		for (int i=0; i<sum.length; ++i) sum[i]=0;
		int c=src.getRGB(x,y);
		//float c=rgb2gray(src.getRGB(x,y));
		for (int my=0; my<kern.getHeight(); ++my) {
			int py=y+my+dy;
			for (int mx=kern.getWidth(); mx<kern.getWidth(); ++mx){
				int px=x+mx+dx;
				double k=kern.get(mx,my);
				if (px<0||px>=src.getWidth()|| py<0||py>=src.getHeight())
					rgbAdd(sum,c,k);
				else{
					rgbAdd(sum,src.getRGB(px,py),k);
				}
			}
		}
	}
	public static void convolve(BufferedImage src,BufferedImage dst,Matrix kern) {
		double[] sum=new double[3];
		int w2=kern.getWidth()/2,h2=kern.getHeight()/2;
		for (int y=0; y<src.getHeight(); ++y)
			for (int x=0; x<src.getWidth(); ++x) {
				convolvexy(sum,src, x-w2, y-h2, kern);
				dst.setRGB(x,y,rgb(sum));
			}
	}
	public static void sobel(BufferedImage src,BufferedImage dst) {
		double[] sumx=new double[3];
		double[] sumy=new double[3];
		Matrix kx=new Matrix(3,3);
		kx.set(0,0,-1);kx.set(2,0,1);
		kx.set(0,1,-2);kx.set(2,1,2);
		kx.set(0,2,-1);kx.set(2,2,1);
		Matrix ky=kx.transposed();
		//ky.set(0,0,-1);ky.set(1,0,-2);ky.set(2,0,-1);
		//ky.set(0,2,1);ky.set(1,2,2);ky.set(2,2,1);

		for (int y=0; y<src.getHeight(); ++y)
			for (int x=0; x<src.getWidth(); ++x) {
				convolvexy(sumx,src, x, y, kx);
				convolvexy(sumy,src, x, y, ky);
				//dst.setRGB(x,y,gray2rgb((float)Math.sqrt(dx*dx+dy*dy)));
				//gives similar result to above, but is much faster
				//dst.setRGB(x,y,gray2rgb(Math.abs(dx)+Math.abs(dy))));
				sumx[0]=Math.abs(sumx[0])+Math.abs(sumy[0]);
				sumx[1]=Math.abs(sumx[1])+Math.abs(sumy[1]);
				sumx[2]=Math.abs(sumx[2])+Math.abs(sumy[2]);
				dst.setRGB(x,y,rgb(sumx));
			}
	}
}
