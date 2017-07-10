package algebra;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import sys.Const;
import sys.Logger;
import sys.StrUtil;

public class SqrFitt {
	static final Logger log=Logger.getLogger();
	public static interface FittPar{
		public double calc(double x);
	}
	public static double hypot(double a, double b) {
		double r;
		if (Math.abs(a) > Math.abs(b)) {
		   r = b/a;
		   r = Math.abs(a)*Math.sqrt(1+r*r);
		} else if (b != 0) {
		   r = a/b;
		   r = Math.abs(b)*Math.sqrt(1+r*r);
		} else {
		   r = 0.0;
		}
		return r;
	}

	public static class StatPar{
		public long samples;
		public double minVal,maxVal;
		public double meanVal;
		public double rmsVal;//root mean square SQRT(SUM(xi^2)/n)
	}
	public static class PolyPar extends StatPar implements FittPar {
		private double coef[];
		public void setSize(int n){
			if (coef==null) coef=new double[n];
		}
		public int size(){return coef==null?0:coef.length;}
		public void set(int i,double v){
			if (coef==null || i<0||i>=coef.length) return ;
			coef[i]=v;;
		}
		public double get(int i){
			if (coef==null || i<0||i>=coef.length) return Double.NaN;
			return coef[i];
		}
		@Override
		public double calc(double x){
			if (coef==null) return x;
			double y=0,a=1.0;
			for (int i=0; i<coef.length; ++i){
				y+=a*coef[i]; a*=x;
			}
			return y;
		}
		@Override
		public String toString(){
			if (coef==null) return "";
			StringBuilder b=new StringBuilder(coef.length*3);
			for (int i=0; i<coef.length; ++i){
				if (i==0) b.append(String.format("%+g",coef[i]));
				else if (i==1) b.append(String.format("%+gx",coef[i]));
				else b.append(String.format("%+gx^%d",coef[i],i));
			}
			return b.toString();
		}
	}
	/**
	 * Y = amp*exp(-alpha*(x-mean)^2)
	 */
	public static class GaussPar extends StatPar implements FittPar {
		static final double SQRT2LN2=Math.sqrt(2*Math.log(2));
		public double alpha;
		public double x0;
		public double amp;
		public double offs;
		/**
		 * Full Width at Half Maximum<br>
		 * FWHM = 2&radic;(2ln(2)) * sigma
		 */
		public double fwhm(){
			if (alpha>0.0) return 2*SQRT2LN2*Math.sqrt(sigmaSq());
			return -2*SQRT2LN2*Math.sqrt(-sigmaSq());
		}
		/**
		 * alpha=1/(2*sigma^2), sigma=sqrt(0.5/alpha)
		 * sigma^2 = variance
		 * @return
		 */
		public double sigmaSq(){return 0.5/alpha;}
		@Override
		public double calc(double x) {
			x-=x0;
			return offs+amp*Math.exp(-alpha*x*x);
		}
	}
	public static class ExpPar extends StatPar implements FittPar {
		public double alpha;
		public double x0;
		public double offs;
		@Override
		public double calc(double x) {
			return offs+Math.exp(alpha*(x-x0));
		}
	}
	/**
	 * Vertical fitting to polynomian
	 * @param deg
	 * @param vp list of (x,y) pairs {@link Point2D}
	 * @param s start from index
	 * @param n number of elements
	 * @param coef calculated coefficients
	 */
	public static void poly(int deg, List<Point2D> vp,int s,int n,PolyPar par){
		getDistribPar(vp,par);
		Matrix m=new Matrix(deg+2,deg+1);
		double[] sum=new double[2*deg+1];
		double x1,x2;
		int i,j;
		sum[0]=n;
		for (i=0; i<n; ++i){
			Point2D p=vp.get((s+i)%vp.size());
			m.set(deg+1, 0, m.get(deg+1,0)+p.getY());
			x1 = p.getX();
			x2 = x1;
			for (j = 0; j < 2*deg; ++j){
				sum[j + 1] += x2;
				if (j < deg) m.set(deg+1,j+1,m.get(deg+1,j+1)+x2*p.getY());
				x2 *= x1;
			}
		}
		for (j = 0; j < deg; ++j){
			x1 = sum[j];
			x2 = sum[2*deg - j];
			for (i = 0; i <= j; ++i){
				m.set(i,j-i,x1);
				m.set(deg-i,deg-j+i,x2);
			}
		}
		x1 = sum[j]; sum=null;
		for (i = 0; i <= j; ++i) m.set(i,j-i,x1);
		if (par.coef==null || par.coef.length!=deg+1)
			par.coef=new double[deg+1];
		//log.info("solving matrix\n%s",m.toString());
		m.solve(par.coef);
		//log.info("solved matrix\n%s",m.toString());
		m.dispose();
	}
	public static double errorDist(Polygon p,int s,int n,double[] coef){
		double rmax=0,r,x,y;
		int xi;
		if (Double.isNaN(coef[coef.length-1])){
			x=coef[0];
			for (int i=0; i<n; ++i){
				xi=p.xpoints[(s+i)%p.npoints];
				r=Math.abs(xi-x);
				if (rmax<r) rmax=r;
			}
			return rmax;
		}
		for (int i=0; i<n; ++i){
			xi=p.xpoints[(s+i)%p.npoints];
			y=0;x=1;
			for (int j=0; j<coef.length; ++j) {y+=coef[j]*x; x*=xi;}
			r=Math.abs(p.ypoints[(s+i)%p.npoints]-y);
			r=r/Math.sqrt(1+coef[1]*coef[1]);
			if (rmax<r) rmax=r;
		}
		return rmax;
	}
	/**
	 * square of perpendicular differences
	 * R^2= sum((yi-(axi+b))^2/(1+b^2))
	 * 1..n
	 *
	 */
	public static void line(Polygon p,int s,int n,double[] coef){
		double sx,sy,sxy,sx2,sy2;
		double x,y,r;
		sx=sy=sxy=sx2=sy2=0;
		for (int i=0; i<n; ++i){
			x=p.xpoints[(s+i)%p.npoints];
			y=p.ypoints[(s+i)%p.npoints];
			sx+=x/n; sx2+=x*x/n;
			sy+=y/n; sy2+=y*y/n;
			sxy+=x*y/n;
		}
		if (Math.abs(sxy-sx*sy)<Const.eps){
			coef[1]=Double.NaN;
			coef[0]=sx;
		}
		else{
			double B=0.5*(sx2 - sy2 - sx*sx  + sy*sy)/(sxy - sx*sy);
			double D=Math.sqrt(B*B+1);
			coef[1]=D-B;
			coef[0]=sy-coef[1]*sx;

			r=errorDist(p,s,n,coef);
			coef[1]=-D-B;
			coef[0]=sy-coef[1]*sx;
			if (r<errorDist(p,s,n,coef)){
				coef[1]=D-B;
				coef[0]=sy-coef[1]*sx;
			}
		}
		/*
		if (Double.isNaN(coef[coef.length-1])){
			coef[0]=0;
			for (int i=0; i<n; ++i) coef[0]+=p.xpoints[(s+i)%p.npoints];
			coef[0]/=n;
		}
		 */
	}
	/**
	 * square of vertical differences
	 * R^2= sum((yi-(axi+b))^2)
	 * 1..n
	 */
	public static void line2(Polygon p,int s,int n,double[] coef){
		int i=0;
		double x1,x2,y1,y2;
		x1=p.xpoints[(s+i)%p.npoints];
		y1=p.ypoints[(s+i)%p.npoints];
		if (n==1) {coef[0]=y1;coef[1]=0;return ;}
		if (n==2){
			x2=p.xpoints[(s+i+1)%p.npoints];
			y2=p.ypoints[(s+i+1)%p.npoints];
		}
		else{
			double sx,sy,sxy,sx2,sy2;
			double x,y;
			sx=sy=sxy=sx2=sy2=0;
			for (i=0; i<n; ++i){
				x=p.xpoints[(s+i)%p.npoints];
				y=p.ypoints[(s+i)%p.npoints];
				sx+=x/n; sx2+=x*x/n;
				sy+=y/n; sy2+=y*y/n;
				sxy+=x*y/n;
			}
			if (Math.abs(n*sx2-sx*sx - n*sy2-sy*sy) > Const.eps) {
				log.error("not the same value !!!");
			}
			coef[1]=(n*sxy-sx*sy)/(n*sx2-sx*sx);
			//coef[1]=(n*sxy-sx*sy)/(n*sy2-sy*sy);
			coef[0]=sy-coef[1]*sx;
			return;
		}
		if (x1==x2){coef[1]=Double.NaN;coef[0]=x1;}
		else {
			coef[1]=(y2-y1)/(x2-x1);
			coef[0]=y1-coef[1]*x1;
		}
	}
	public static void getDistribPar(List<Point2D> vp,StatPar par){
		if (vp.size()==0) {
			return ;
		}
		Point2D p=vp.get(0);
		double y=p.getY()/vp.size();
		par.maxVal=par.minVal=y;
		par.samples=0;
		par.meanVal=par.rmsVal=0;
		double s=0;
		for (int i=0; i<vp.size(); ++i) {
			p=vp.get(i);
			y=p.getY()/vp.size();
			s+=y;
			if (par.minVal>y) par.minVal=y;
			else if (par.maxVal<y) par.maxVal=y;
			par.meanVal+=y*p.getX();
			par.rmsVal+=y*p.getX()*p.getX();
		}
		par.meanVal/=s;
		par.rmsVal/=s;
		par.rmsVal=Math.sqrt(par.rmsVal);
		par.samples=(long)(s*vp.size());
		par.minVal*=vp.size();
		par.maxVal*=vp.size();
		log.debug("samp=%d mean=%.3g rms=%.3g",par.samples,par.meanVal,par.rmsVal);
	}
	/**
	 * fitting:  Y = amp*exp(-alpha*(x-mean)^2), alpha=1/(2*sigma^2)<br>
	 * 1. ln(y) = -alpha*(x-mean)^2+ln(amp)<br>
	 * &nbsp;. ln(y) = (-alpha)*x^2+(2*alpha*mean)*x+(ln(amp)-alpha*mean^2)<br>
	 * 2. fitting poly(degree=2), result is y=Ax^2+Bx+C.<br>
	 * 3. alpha=-A, mean=B/(2*alpha), amp=exp(C+alpha*mean^2)
	 * @param hist list of (x,y) samples
	 * @param par gauss params
	 */
	public static void getGaussPar(List<Point2D> hist,GaussPar par){
		PolyPar ppar=new PolyPar();
		int i,n;
		par.offs = 0;
		getDistribPar(hist,par);
		List<Point2D> val=new ArrayList<Point2D>(hist.size());
		for (i = 0; i < hist.size(); ++i) {
			Point2D p=hist.get(i);
			if (p.getY()>Const.eps) break;
		}
		for (n = hist.size(); n>0; --n) {
			Point2D p=hist.get(n-1);
			if (p.getY()>Const.eps) break;
		}
		for (; i < n; ++i) {
			Point2D p=hist.get(i);
			if (p.getY()<0.0) continue;
			val.add(new Point2D.Double(p.getX(),p.getY()));
		}
		if (val.size()<3){
			Point2D p=hist.get(0);
			hist.add(0,new Point2D.Double(p.getX()-1,par.offs+0.01));
			p=hist.get(hist.size()-1);
			hist.add(new Point2D.Double(p.getX()+1,par.offs+0.01));
		}
		for (i=0; i < val.size(); ++i) {
			Point2D.Double p=(Point2D.Double)val.get(i);
			//val.add(new Point2D.Double(p.getX(),Math.log1p(p.getY()-par.offs)));
			p.y=Math.log1p(p.y-par.offs);
		}

		SqrFitt.poly(2,val,0,val.size(),ppar);
		log.debug("coefs=%s",StrUtil.implode(',',ppar.coef));
		par.alpha = -ppar.coef[2];// sigma=sqrt(0.5/alpha)
		par.x0 = 0.5*ppar.coef[1]/par.alpha; // mean
		par.amp = Math.expm1(ppar.coef[0]+par.alpha*par.x0*par.x0); // A
		log.debug("gauss: alpha=%.3g mean=%.3g amp=%.3g",par.alpha,par.x0,par.amp);
	}
	/**
	 * fitting:  Y = A*exp(alpha*(x-x0))<br>
	 * 1. ln(y) = lnA+alpha*(x-x0)<br>
	 * &nbsp;. ln(y) = alpha*x+lnA-alpha*x0<br>
	 * 2. fit to y=Ax+B<br>
	 * 3. alpha=A, x0=-B/alpha
	 * @param hist list of (x,y) samples
	 * @param par exp params
	 **/
	public static void getExpPar(List<Point2D> hist,ExpPar par){
		PolyPar ppar=new PolyPar();
		int i,n;
		par.offs=0;
		getDistribPar(hist,par);
		//info.offs = info.minVal;
		List<Point2D> val=new ArrayList<Point2D>(hist.size());
		for (i = 0; i < hist.size(); ++i) {
			Point2D p=hist.get(i);
			if (p.getY()>Const.eps) break;
		}
		for (n = hist.size(); n>0; --n) {
			Point2D p=hist.get(n-1);
			if (p.getY()>Const.eps) break;
		}
		for (; i < n; ++i) {
			Point2D p=hist.get(i);
			if (p.getY()<0.0) continue;
			val.add(new Point2D.Double(p.getX(),Math.log1p(p.getY()-par.offs)));
		}
		SqrFitt.poly(1,val,0,val.size(),ppar);
		log.debug("poly: A=%g B=%g",ppar.coef[1],ppar.coef[0]);
		/*double sx,sy,sxy,sx2;
		sx=sy=sxy=sx2=0;
		for (i=0; i<val.size(); ++i){
			Point2D p=val.get(i);
			sx+=p.getX(); sy+=p.getY();
			sx2+=p.getX()*p.getX();
			sxy+=p.getX()*p.getY();
		}
		ppar.coef[0]=(sy*sx2-sx*sxy)/(val.size()*sx2-sx*sx);
		ppar.coef[1]=(val.size()*sxy-sx*sy)/(val.size()*sx2-sx*sx);
		log.debug("exp: A=%g B=%g",ppar.coef[1],ppar.coef[0]);*/
		par.alpha=ppar.coef[1];
		par.x0=-par.alpha/ppar.coef[0];
	}
	public static void getLandauPar(List<Point2D> hist,GaussPar info){
		info.offs = 0;
		getDistribPar(hist,info);
	}
}
