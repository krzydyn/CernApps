package sys.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import sys.Logger;
import sys.SysUtil;

public class DocumentValidator extends DocumentFilter {
	public final static Logger log=Logger.getLogger();

	public final static String USASCII="[-A-Za-z0-9 _]*";
	public final static String DECIMAL_USIGNED="[0-9]*";
	public final static String DECIMAL_SIGNED="[+-]{0,1}[0-9]*";
	public final static String FLOATING_UNSIGNED="[0-9]*\\.{0,1}[0-9]*";
	public final static String FLOATING_SIGNED="[+-]{0,1}[0-9]*\\.{0,1}[0-9]*";
	public final static String HEX_UNSIGNED="[0-9A-Fa-f]*";
	public final static String HEX_SIGNED="[+-]{0,1}[0-9A-Fa-f]*";
	public final static String DATE_YYYYMMDD="[0-9]{0,4}([-/.][0-9]{0,2}){0,2}";
	public final static String TIME_YYYYMMDD_HHMM="[0-9]{0,4}([-/.][0-9]{0,2}){0,2}( [0-9]{0,2}(:[0-9]{0,2}){0,2}){0,1}";
	public final static String TIME_HHMMSS="[0-9]{0,2}(:[0-9]{0,2}){0,2}";
	public final static String IPADDR="[0-9]{0,3}(\\.[0-9]{0,3}){0,3}";

	private String regex;
	private int lmax;
	private int forceCase=0;

	public DocumentValidator setLimit(int lmax)
	{
		this.lmax=lmax;
		return this;
	}
	public DocumentValidator setCase(int c){forceCase=c;return this;}
	public DocumentValidator setUpper() { forceCase=2; return this; }
	public DocumentValidator setLower() { forceCase=1; return this; }
	public DocumentValidator setRegex(String regex)
	{
		//TODO Save regex as compiled Pattern
		this.regex=regex;
		return this;
	}

	protected boolean verify(String s)
	{
		try{
			//Pattern p=Pattern.compile(regex);
			if (s.length()==0) return true;
			if (lmax>0 && s.length()>lmax)
			{
				log.debug("'%s' length > %d",s,lmax);
				return false;
			}
			if (regex!=null)
			{
				if (!s.matches(regex))
				{
					log.debug("'%s' not match '%s'",s,regex);
					return false;
				}
			}
			return true;
		}catch (Exception e) {
			log.debug(e,"'%s' has exception");
			return false;
		}
	}

	@Override
	public void insertString(DocumentFilter.FilterBypass fb, int offs,String str, AttributeSet a) throws BadLocationException {
		Document doc=fb.getDocument();
	    StringBuffer sb=new StringBuffer(doc.getText(0,doc.getLength()));
	    if (forceCase==1) str=str.toLowerCase();
	    else if (forceCase==2) str=str.toUpperCase();
	    sb.insert(offs,str);
	    if (!verify(sb.toString())) SysUtil.beep();
	    else super.insertString(fb, offs, str, a);
	}

	@Override
	public void replace(DocumentFilter.FilterBypass fb, int offs, int len,String str, AttributeSet a) throws BadLocationException {
		Document doc=fb.getDocument();
	    StringBuffer sb=new StringBuffer(doc.getText(0,doc.getLength()));
	    if (forceCase==1) str=str.toLowerCase();
	    else if (forceCase==2) str=str.toUpperCase();
	    sb.replace(offs, offs+len, str);
	    if (!verify(sb.toString())) SysUtil.beep();
	    else super.replace(fb, offs, len, str, a);
	}

	@Override
	public void remove(DocumentFilter.FilterBypass fb, int offs, int len) throws BadLocationException {
		Document doc=fb.getDocument();
	    StringBuffer sb=new StringBuffer(doc.getText(0,doc.getLength()));
	    sb.delete(offs, offs+len);
	    if (!verify(sb.toString())) SysUtil.beep();
	    else super.remove(fb, offs, len);
	}
}
