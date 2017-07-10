package algebra;

import sys.Const;

/**
 * Matrix
 */
public class Matrix {
	public static final int TYPE_ZERO = 0;
	public static final int TYPE_IDENTITY = 1;
	public static final int TYPE_TRANSLATION = 2;
	public static final int TYPE_SCALE = 4;
	public static final int TYPE_COMPLEX = 8;

	//TODO implement MatrixD,MatrixF,MatrixI for double,float,int
	protected static Matrix tmp=new Matrix(3,3);
	protected double[] mtx=null;
	protected int w,h;
	private int type;
	/**
	 * Create matrix (identity)
	 */
	public Matrix(int w,int h) {
		setDim(w,h);
	}
	public Matrix(Matrix m) {set(m);}
	public void dispose(){mtx=null;}
	@Override
	public Object clone() { return new Matrix(this); }
	public void setDim(int w,int h) {
		this.w=w;this.h=h;
		int n=w*h;
		if (mtx==null||mtx.length<n) mtx=new double[n];
	}
	public int getWidth(){return w;}
	public int getHeight(){return h;}
	public int getType() { return type; }
	public void set(Matrix m) {
		setDim(m.w,m.h);
		System.arraycopy(m.mtx,0,mtx,0,w*h);
		type=m.type;
	}
	public Matrix setTranslation(double dx,double dy) {
		if (w!=3||h!=3) throw new RuntimeException("invalid matrix");
		identity();
		if (Math.abs(dx)>Const.eps || Math.abs(dy)>Const.eps){
			set(2,0,dx); set(2,1,dy);
			type=TYPE_TRANSLATION;
		}
		return this;
	}
	public void set(int x,int y,double a) { mtx[w*y+x]=a; }
	public double get(int x,int y) {
		if (x<0 || x>=w || y<0 ||y>=h) return 0.0;
		return mtx[w*y+x];
	}
	//used for re-sampling
	public double get(float i,float j,float r) {
		if (i<0 || i>=w || j<0 ||j>=h) return 0.0;
		double s=0;
		double f=0;
		int rr=(int)Math.ceil(r);
		for (int ii=-rr; ii<=rr; ++ii)
			for (int jj=-rr; jj<=rr; ++jj){
				double d=ii*ii+jj*jj;
				double x=Math.exp(-d/10);
				s+=get((int)i+ii,(int)j+jj)*x;
				f+=x;
			}
		return s/f;
	}
	public double maxValue() {
		double m=Double.MIN_VALUE;
		int n=w*h;
		for (int i=0; i<n; ++i) {if (m<mtx[i])m=mtx[i];}
		return m;
	}
	public double maxValueY(int x) {
		double m=mtx[x];
		int n=w*h;
		for (int i=x+w; i<n; i+=w) {if (m<mtx[i])m=mtx[i];}
		return m;
	}
	public double maxValueX(int y) {
		double m=mtx[w*y];
		int n=w*y+w;
		for (int i=w*y+1; i<n; ++i) {if (m<mtx[i])m=mtx[i];}
		return m;
	}
	public Matrix assign(Matrix m){ set(m); return this; }
	/**
	 * make zero matrix (all elements set zero)
	 */
	public Matrix zero(){
		int n=w*h;
		for (int i=0; i<n; i++) mtx[i]=0.0;
		type=TYPE_ZERO;
		return this;
	}
	/**
	 * make identity matrix (ones on diagonal, other zeros)
	 * <table>
	 * <tr><td rowspan="3">M = <td>1<td>0<td>0
	 * <tr><td>0<td>1<td>0
	 * <tr><td>0<td>0<td>1
	 * </table>
	 */
	public Matrix identity() {
		zero();
		int n=Math.min(w,h);
		for (int i = 0; i < n; i++) set(i,i,1.0);
		type=TYPE_IDENTITY;
		return this;
	}
	public Matrix add(int i,int j,double a) {
		if (i<0 || i>=w || j<0 ||j>=h) return this;
		a+=get(i,j); set(i,j,a);
		return this;
	}
	public Matrix add(Matrix m) {
		int mw=Math.min(m.w, w);
		int mh=Math.min(m.h, h);
		for (int i=0; i<mw; i++)
			for (int j=0; j<mh; j++)
				{ set(i,j,get(i,j)+m.get(i,j)); }
		updateType();
		return this;
	}
	public Matrix sub(Matrix m) {
		int mw=Math.min(m.w, w);
		int mh=Math.min(m.h, h);
		for (int i=0; i<mw; i++)
			for (int j=0; j<mh; j++)
				{ set(i,j,get(i,j)-m.get(i,j)); }
		updateType();
		return this;
	}
	public Matrix mul(double m) {
		int n=w*h;
		for (int i=0; i<n; ++i) mtx[i]*=m;
		updateType();
		return this;
	}
	/**
	 * <table>
	 * <tr><td>[this] =<td>[this]<td>*<td>[m]
	 * </table>
	 * <pre>IKJ
for (i = 0; i < N; i++)
	for (k = 0; k < N; k++)
		for (j = 0; j < N; j++)
			C[i*N+j] += A[i*N+k]*B[k*N+j];</pre>
	 */
	public Matrix mul(Matrix m) {
		double a;
		synchronized (tmp) {
		tmp.setDim(w, h);
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i) {
				a=0.0;
				for (int k=0; k<w; k++) a+=get(k,j)*m.get(i,k);
				tmp.set(i,j,a);
			}
		assign(tmp);
		}
		updateType();
		return this;
	}
	/**
	 * <table>
	 * <tr><td>[this] =<td>[m]<td>*<td>[this]
	 * </table>
	 */
	public Matrix mulLeft(Matrix m) {
		double a;
		synchronized (tmp) {
		tmp.setDim(w, h);
		for (int j=0; j<h; j++)
			for (int i=0; i<w; i++) {
				a=0.0;
				for (int k=0; k<w; k++) a+=m.get(k,j)*get(i,k);
				tmp.set(i,j,a);
			}
		assign(tmp);
		}
		updateType();
		return this;
	}
	/**
	 * quick matrix power
	 * @param n
	 * @return
	 */
	public Matrix pow(int n) {
		Matrix y=new Matrix(w,h);
		y.identity();
		while(n!=0) {
	        if((n&1)!=0) y.mul(this);
	        mul(this);
	        n>>>=1;
	    }
		this.assign(y);
		return this;
	}

	public Matrix upper(int limit) {
		double d,df=0;
		for (int i=0; i<limit; ++i) {
			int j;
		    for (j=i; j < h; ++j) {
		      df=get(i,j);
		      if (Math.abs(df) > Const.eps) break;
		      set(i,j,0);
		    }
		    //if (j!=i) swap rows i,j
		    for (j=j+1; j < h; ++j) {
		      d=get(i,j)/df;
		      for (int k=i+1; k < w; ++k)
		    	  set(k,j,get(k,j)-get(k,i)*d);
		    }
		    for (j=i+1; j < h; ++j) set(i,j,0);
		}
		return this;
	}
	public Matrix lower(int limit) {
		double d,df=0;
		for (int i=1; i<limit; ++i) {
			int j;
		    for (j=0; j < i; ++j) {
		    	if (Math.abs(get(i,j)) > Const.eps) break;
		    	set(i,j,0);
		    }
		    //if (j!=i) swap rows i,j
		    df=get(i,i);
		    if (Math.abs(df) < Const.eps) {set(i,i,0);continue;}
		    for (; j < i; ++j) {
		      d=get(i,j)/df;
		      for (int k=i+1; k < w; k++)
		    	  set(k,j,get(k,j)-get(k,i)*d);
		    }
		    for (j=0; j < i; j++) set(i,j,0);
		}
		return this;
	}
	public void solve(double[] res) {
		int n=Math.min(w-1,h);
		for (int i=0; i<res.length; ++i) res[i]=0;
		//Logger.getInstance().debug("solve matrix:\n%s",toString());
		upper(n);
		lower(n);
		//Logger.getInstance().debug("solved matrix:\n%s",toString());
		for (int i=0; i<h && i<res.length; ++i)
			res[i]=get(w-1,i)/get(i,i);
	}

	/**
	 * @return matrix determinant
	 */
	public double det() {
		int n=Math.min(w,h);
		if (n==1) return get(0,0);
		if (n==2) return get(0,0)*get(1,1)-get(1,0)*get(0,1);
		double d=1.0;
		synchronized(tmp) {
		tmp.set(this);
		tmp.upper(n);
		for (int i=0; i < n; ++i) d*=tmp.get(i,i);
		}
		return d;
	}
	final protected void removeij(int i0,int j0,Matrix m) {
		int jj,ii;
		for (int j=jj=0; j<m.h; ++j) {
			if (j==j0) continue;
			for (int i=ii=0; i<m.w; ++i) {
				if (i==i0) continue;
				set(ii,jj,m.get(i,j));
				++ii;
			}
			++jj;
		}
	}
	public void transpose(){
		synchronized (tmp) {
			tmp.assign(this);
			transposeFrom(tmp);
		}
	}
	public void transposeFrom(Matrix m) {
		setDim(m.getHeight(),m.getWidth());
		for (int j=0; j<m.h; ++j)
			for (int i=0; i<m.w; ++i)
				set(j,i,m.get(i,j));
	}
	public Matrix transposed() {
		Matrix m=new Matrix(h,w);
		m.transposeFrom(this);
		return m;
	}
	public void joinFrom(Matrix m) {
		setDim(m.getHeight(),m.getWidth());
		if (w==1&&h==1) {set(0,0,m.get(0,0));return;}
		if (w==2&&h==2) {
			set(0,0,m.get(1,1));
			set(1,0,-m.get(1,0));
			set(0,1,-m.get(0,1));
			set(1,1,m.get(0,0));
			return ;
		}
		Matrix t=new Matrix(m.w-1,m.h-1);
		for (int j=0; j<m.h; ++j)
			for (int i=0; i<m.w; ++i) {
				t.removeij(i,j,m);
				if ((i+j)%2==0) set(j,i,t.det());
				else set(j,i,-t.det());
			}
		t=null;
	}
	public Matrix joined() {
		Matrix m=new Matrix(h,w);
		m.joinFrom(this);
		return m;
	}
	public Matrix invert() {
		if (w!=h) throw new RuntimeException("inalid matrix");
		synchronized (tmp) {
		tmp.setDim(2*w, h);
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i)
				tmp.set(i,j,get(i,j));
		for (int j=0; j<h; ++j)
			for (int i=0; i<w; ++i)
				if (j==i) tmp.set(w+i,j,1);
				else tmp.set(w+i,j,0);
		tmp.upper(w);
		tmp.lower(w);
		for (int j=0; j<h; ++j) {
			double d=tmp.get(j, j);
			for (int i=0; i<w; ++i)
				set(i,j,tmp.get(w+i,j)/d);
		}
		}
		return this;
	}
	public Matrix inverted() {
		if (w!=h) throw new RuntimeException("invalid matrix");
		return new Matrix(this).invert();
	}
	@Override
	public String toString() {
		StringBuilder b=new StringBuilder();
		for (int j=0; j<h; j++)
		{
			b.append("|");
			for (int i=0; i<w; i++) b.append(get(i,j)+",");
			b.deleteCharAt(b.length()-1);
			b.append("|\n");
		}
		if (b.length()>0) b.deleteCharAt(b.length()-1);
		return b.toString();
	}
	private void updateType() {
		type=TYPE_COMPLEX;
		if (get(1,0)!=0||get(0,1)!=0) return ;
		type=TYPE_SCALE;
		if (get(0,0)!=1.0 || get(1,1)!=1.0) return ;
		type=TYPE_TRANSLATION;
		if (get(2,0)!=0||get(2,1)!=0) return ;
		type=TYPE_IDENTITY;
	}
}
