package sys.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.io.IOException;
import java.text.AttributedString;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.StyleConstants.CharacterConstants;
import javax.swing.text.StyleConstants.ColorConstants;
import javax.swing.text.StyleConstants.FontConstants;
import javax.swing.text.StyleConstants.ParagraphConstants;
import javax.swing.text.StyleContext.NamedStyle;

import sys.Logger;

public class EnchancedEditor {
	static Logger log=Logger.getLogger();

	public static HashMap<String, Action> getActionMap(JTextComponent tc){
		HashMap<String, Action> actions = new HashMap<String, Action>();
	    Action[] actionsArray = tc.getActions();
	    for (int i = 0; i < actionsArray.length; i++) {
	        Action a = actionsArray[i];
	        actions.put((String)a.getValue(Action.NAME), a);
	    }
	    return actions;
	}
	public static String[] getFontList(){
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

		//Get the font names from the graphics environment
		return env.getAvailableFontFamilyNames();
	}
	public static void setStyleKeyBinding(JTextComponent txt){
		//TODO add Listener to the action (document changeUpdate)
		KeyStroke key;
		InputMap inputMap = txt.getInputMap();
		key = KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK);
		//inputMap.put(key, new StyledEditorKit.BoldAction());
		inputMap.put(key, new StyledEditorKit.BoldAction());
		key = KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK);
		inputMap.put(key, new StyledEditorKit.ItalicAction());
		key = KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK);
		inputMap.put(key, new StyledEditorKit.UnderlineAction());

		key = KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.ALT_MASK);
		inputMap.put(key, new StyledEditorKit.AlignmentAction("Left",StyleConstants.ALIGN_LEFT));
		key = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.ALT_MASK);
		inputMap.put(key, new StyledEditorKit.AlignmentAction("Center",StyleConstants.ALIGN_CENTER));
		key = KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.ALT_MASK);
		inputMap.put(key, new StyledEditorKit.AlignmentAction("Right",StyleConstants.ALIGN_RIGHT));
		key = KeyStroke.getKeyStroke(KeyEvent.VK_J, Event.ALT_MASK);
		inputMap.put(key, new StyledEditorKit.AlignmentAction("Just",StyleConstants.ALIGN_JUSTIFIED));

		//key = KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK);
		//inputMap.put(key, StyledEditorKit.selectAllAction);
	}
	public static AttributedString getAttributedString(StyledDocument doc){
		AttributedString attString = null;
        try {
            attString = new AttributedString(doc.getText(0, doc.getLength()));
        } catch (BadLocationException ex) {
        }
		Element root = doc.getDefaultRootElement();
		for (int lpParagraph=0; lpParagraph<root.getElementCount(); lpParagraph++){
			final Element line = root.getElement(lpParagraph);
	        for (int i = 0; i < line.getElementCount(); i++) {
	            final Element element = line.getElement(i);

	            final AttributeSet as = element.getAttributes();
	            final Enumeration<?> ae = as.getAttributeNames();

	            while (ae.hasMoreElements()) {
	                final Object attrib = ae.nextElement();
	                if (attrib == ColorConstants.Foreground) {
	                    //Foreground
	                    attString.addAttribute(TextAttribute.FOREGROUND,
	                            as.getAttribute(attrib), element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == ColorConstants.Background) {
	                    //Background
	                    attString.addAttribute(TextAttribute.BACKGROUND,
	                            as.getAttribute(attrib), element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == FontConstants.Bold) {
	                    //Bold
	                    attString.addAttribute(TextAttribute.WEIGHT,
	                            TextAttribute.WEIGHT_BOLD, element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == CharacterConstants.Bold) {
	                    //Bold
	                    attString.addAttribute(TextAttribute.WEIGHT,
	                            TextAttribute.WEIGHT_BOLD, element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == FontConstants.Family) {
	                    //Family
	                    attString.addAttribute(TextAttribute.FAMILY,
	                            as.getAttribute(attrib), element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == FontConstants.Italic) {
	                    //italics
	                    attString.addAttribute(TextAttribute.POSTURE,
	                            TextAttribute.POSTURE_OBLIQUE, element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == FontConstants.Size) {
	                    attString.addAttribute(TextAttribute.SIZE,
	                    		as.getAttribute(attrib), element.getStartOffset(), element.getEndOffset());
	                } else if (attrib == CharacterConstants.Underline) {
	                    //Underline
	                    attString.addAttribute(TextAttribute.UNDERLINE,
	                            TextAttribute.UNDERLINE_ON, element.getStartOffset(), element.getEndOffset());
	                } else log.debug("wrong attr=%s",attrib.toString());
	            }
	        }
	        //attString.addAt
		}
        return attString;
	}
	private static void buildParagraphString(AttributeSet attr,StringBuffer attrstr){
		for (Enumeration<?> i=attr.getAttributeNames(); i.hasMoreElements(); ){
			Object key=i.nextElement();
			Object val=attr.getAttribute(key);
			if (key instanceof ParagraphConstants || key instanceof FontConstants)
				attrstr.append(key.toString()+"="+val.toString());
			else if (key instanceof ColorConstants)
				attrstr.append(String.format("%s=%06X",key.toString(),((Color)val).getRGB()&0xffffff));
			else if (val instanceof NamedStyle){
				NamedStyle ns=(NamedStyle)val;
				val=ns.getAttribute(StyleConstants.NameAttribute);
				if (val==null || !val.equals("default"))
					buildParagraphString(ns,attrstr);
				continue;
			}
			else if (key instanceof String){
				//if (key.equals("FONT_ATTRIBUTE_KEY")) ;
			}
			else {
				log.debug("unsupp key(%s)='%s', val='%s'",key.getClass().getName(),key,val);
				continue;
			}
			attrstr.append(',');
		}
	}
	private static void buildCharacterString(AttributeSet attr,StringBuffer attrstr,Font df){
		for (Enumeration<?> i=attr.getAttributeNames(); i.hasMoreElements(); ){
			Object key=i.nextElement();
			Object val=attr.getAttribute(key);
			if (key instanceof CharacterConstants || key instanceof FontConstants){
				String k=key.toString();
				boolean add=true;
				if (df!=null){
					if (k.equals("size")) add=!val.equals(df.getSize2D());
					else if (k.equals("family")) add=!val.equals(df.getFamily());
				}
				if (!add) continue;
				attrstr.append(key.toString()+"="+val.toString());
			}
			else if (key instanceof ColorConstants){
				//log.debug("get key:%s='%s', val='%s'",key.getClass(),key,val);
				attrstr.append(String.format("%s=%06X",key.toString(),((Color)val).getRGB()&0xffffff));
			}
			else {
				log.debug("unsupp key(%s)='%s', val='%s'",key.getClass().getName(),key,val);
				continue;
			}
			attrstr.append(',');
		}
	}
	public static void getDocument(List<Object> dst,StyledDocument src, int selectionStart,int selectionEnd) throws IOException{
		Element root,paragraphElement,textElement;
		StringBuffer attrstr=new StringBuffer();
		AttributeSet attr;
		int startOffset, endOffset;

		//save full string
		try{
			dst.add(src.getText(selectionStart, selectionEnd-selectionStart));
		}catch (Exception e) {}

		//collect attributes
		root = src.getDefaultRootElement();
		Font df=src.getFont(root.getAttributes());
		for (int lpParagraph=0; lpParagraph<root.getElementCount(); lpParagraph++){
			paragraphElement = root.getElement(lpParagraph);

			int attrp;
			//Check if the paragraph need to be copy
			if(paragraphElement.getEndOffset() < selectionStart){
				continue; //Go to the next paragraph
			}
			if(paragraphElement.getStartOffset() > selectionEnd){
				break; //Exit the boucle
			}

			attr=paragraphElement.getAttributes();
			if (attr.getAttributeCount()>0){
				startOffset = paragraphElement.getStartOffset();
				endOffset = paragraphElement.getEndOffset();
				attrp=attrstr.length();
				buildParagraphString(attr,attrstr);
				if (attrp<attrstr.length()){
					attrstr.insert(attrp,String.format("p%d,%d,",startOffset-selectionStart,endOffset-startOffset));
					attrstr.setCharAt(attrstr.length()-1, ';');
				}
			}


			for (int lpText=0; lpText<paragraphElement.getElementCount(); lpText++) {
				//Insert a Element in the new Document
				textElement = paragraphElement.getElement(lpText);

				//Check if the Element need to be copy
				if(textElement.getEndOffset() < selectionStart){
					continue; //Go to the next Element
				}
				if(textElement.getStartOffset() > selectionEnd){
					break; //Exit the boucle
				}

				//Find the value of startOffset and endOffset
				if(textElement.getStartOffset() < selectionStart){
					startOffset = selectionStart;
				}else{
					startOffset = textElement.getStartOffset();
				}
				if(textElement.getEndOffset() > selectionEnd){
					endOffset = selectionEnd;
				}else{
					endOffset = textElement.getEndOffset();
				}

				attr = textElement.getAttributes();
				if (attr.getAttributeCount()>0){
					attrp=attrstr.length();
					buildCharacterString(attr,attrstr,df);
					if (attrp<attrstr.length()){
						attrstr.insert(attrp,String.format("c%d,%d,",startOffset-selectionStart,endOffset-startOffset));
						attrstr.setCharAt(attrstr.length()-1, ';');
					}
				}
			}
		}
		if (attrstr.length()>0){
			//log.debug("attr: %s",attrstr.toString());
			dst.add(attrstr.toString());
		}
	}
	public static void setDocument(List<?> src, StyledDocument dst, int pos) throws IOException{
		StringBuffer attrstr;
		SimpleAttributeSet attr;
		int offs, len;
		try{
			dst.remove(0,dst.getLength());
		}catch (Exception e) {
			log.debug(e);
			return ;
		}

		try{
			String s=(String)src.get(0);
			//log.debug("txt:\"%s\"",StrUtil.vis(s));
			dst.insertString(0, s, null);
		}catch (BadLocationException e) {}
		if (src.size()<=1) return ;
		attrstr=new StringBuffer((String)src.get(1));
		//log.debug("attr %s",attrstr.toString());
		int r;
		while(attrstr.length()>0){
			char b=attrstr.charAt(0); attrstr.deleteCharAt(0);
			if ((r=attrstr.indexOf(","))<0) break;
			offs=Integer.parseInt(attrstr.substring(0,r)); attrstr.delete(0, r+1);
			if ((r=attrstr.indexOf(","))<0) break;
			len=Integer.parseInt(attrstr.substring(0,r)); attrstr.delete(0, r+1);
			if ((r=attrstr.indexOf(";"))<0) break;
			if (r==0) continue;
			String[] p=attrstr.substring(0,r).split(","); attrstr.delete(0, r+1);
			if (p.length==0) continue;
			attr=new SimpleAttributeSet();
			for(int i=0; i<p.length; ++i){
				//log.debug("attr: %s",p[i]);
				String[] kv=p[i].split("=");
				if (kv.length<2) continue;
				if ("bold".equals(kv[0])) StyleConstants.setBold(attr,Boolean.parseBoolean(kv[1]));
				else if ("italic".equals(kv[0])) StyleConstants.setItalic(attr,Boolean.parseBoolean(kv[1]));
				else if ("underline".equals(kv[0])) StyleConstants.setUnderline(attr,Boolean.parseBoolean(kv[1]));
				else if ("Alignment".equals(kv[0])) StyleConstants.setAlignment(attr,Integer.parseInt(kv[1]));
				else if ("family".equals(kv[0])) StyleConstants.setFontFamily(attr,kv[1]);
				else if ("size".equals(kv[0])) StyleConstants.setFontSize(attr,Integer.parseInt(kv[1]));
				else if ("foreground".equals(kv[0])) StyleConstants.setForeground(attr,new Color(Integer.parseInt(kv[1],16)));
				else {log.debug("attr: %s=%s ?",kv[0],kv[1]);continue;} // unknown attribute
			}
			if (b=='p') dst.setParagraphAttributes(offs, len, attr, true);
			else if (b=='c') dst.setCharacterAttributes(offs, len, attr, false);
			else log.debug("wrong type: %c",b);
		}
	}
	public static StyledDocument copyDocument(StyledDocument dst, StyledDocument src, int selectionStart,int selectionEnd){
		Element root, paragraphElement, textElement;
		SimpleAttributeSet attr;
		int startOffset, endOffset;

		if (dst==null) dst=new DefaultStyledDocument();
		try{
			dst.remove(0,dst.getLength());
			dst.insertString(0,src.getText(selectionStart,selectionEnd),null);
		}catch (Exception e) {
			log.debug(e);
			return null;
		}
		root = src.getDefaultRootElement();
		for (int lpParagraph=0; lpParagraph<root.getElementCount(); lpParagraph++){
			paragraphElement = root.getElement(lpParagraph);

			//Check if the paragraph need to be copy
			if(paragraphElement.getStartOffset() > selectionEnd){
				break; //Exit the boucle
			}
			if(paragraphElement.getEndOffset() < selectionStart){
				continue; //Go to the next paragraph
			}
			attr = new SimpleAttributeSet(paragraphElement.getAttributes());
			startOffset = paragraphElement.getStartOffset();
			endOffset = paragraphElement.getEndOffset();
			if(startOffset < selectionStart) startOffset = selectionStart;
			if(endOffset > selectionEnd) endOffset = selectionEnd;
			dst.setParagraphAttributes(startOffset-selectionStart, (endOffset-startOffset), attr, true);

			for (int lpText=0; lpText<paragraphElement.getElementCount(); lpText++) {
				//Insert a Element in the new Document
				textElement = paragraphElement.getElement(lpText);

				//log.debug("text %d: %s",lpText,textElement.toString());

				//Check if the Element need to be copy
				if(textElement.getStartOffset() > selectionEnd){
					break; //Exit the boucle
				}
				if(textElement.getEndOffset() < selectionStart){
					continue; //Go to the next Element
				}

				attr = new SimpleAttributeSet(textElement.getAttributes());

				//Find the value of startOffset and endOffset
				if(textElement.getStartOffset() < selectionStart){
					startOffset = selectionStart;
				}else{
					startOffset = textElement.getStartOffset();
				}
				if(textElement.getEndOffset() > selectionEnd){
					endOffset = selectionEnd;
				}else{
					endOffset = textElement.getEndOffset();
				}

				dst.setCharacterAttributes(startOffset-selectionStart,endOffset-startOffset, attr, false);
			}
		}
		return dst;
	}

	public static AttributeSet getAttributes(StyledDocument doc, int pos){
		return doc.getCharacterElement(pos).getAttributes();
	}

	@SuppressWarnings("serial")
	public static class FontListRenderer extends DefaultListCellRenderer  {
	    @Override
		public Component getListCellRendererComponent(JList list,
	                                                  Object value,
	                                                  int index,
	                                                  boolean isSelected,
	                                                  boolean cellHasFocus)  {
	        //Get the default cell renderer
	        JLabel label = (JLabel) super.getListCellRendererComponent(list,
	                                                                   value,
	                                                                   index,
	                                                                   isSelected,
	                                                                   cellHasFocus);

	        //Create a font based on the item value
	        Font itemFont = new Font((String) value, Font.PLAIN, 12);

	        if (itemFont.canDisplayUpTo((String) value) == -1)  {
	                //Set the font of the label
	                label.setFont(itemFont);
	        }
	        else  {
	                //Set the font of the label
	                //label.setFont(label.getFont().deriveFont(Font.PLAIN,16));
	        }

	        return label;
	    }
	}
	public static AttributeSet resolveAttr(AttributeSet attr){
		SimpleAttributeSet dst=new SimpleAttributeSet();
		for (Enumeration<?> i=attr.getAttributeNames(); i.hasMoreElements(); ){
			Object key=i.nextElement();
			Object val=attr.getAttribute(key);
			if (val instanceof NamedStyle){
				NamedStyle ns=(NamedStyle)val;
				dst.addAttributes(ns.copyAttributes());
			}
			else dst.addAttribute(key, val);
		}
		return dst;
	}

	public static Shape getTextShape(Graphics2D g, String str, Font font){
		FontRenderContext frc = g.getFontRenderContext();
		TextLayout tl = new TextLayout(str, font, frc);
		return tl.getOutline(null);
	}
    static public void fitColumnWidth(JTable jtab,int wmax){
    	int rowCnt=jtab.getRowCount();
    	int colCnt=jtab.getColumnCount();
    	int margin=jtab.getColumnModel().getColumnMargin();

		for (int i=0; i<colCnt; i++){
			TableCellRenderer rend;
			Component c;
			Object v;
			int w=0;
			if ((v=jtab.getColumnName(i))!=null){
				rend=jtab.getTableHeader().getDefaultRenderer();
				c=rend.getTableCellRendererComponent(jtab,v,false,false,0,0);
				w=c.getPreferredSize().width;
			}
			for (int j=0; j<rowCnt; j++){
				if ((v=jtab.getValueAt(j,i))==null) continue;
				rend=jtab.getCellRenderer(j,i);
				c=rend.getTableCellRendererComponent(jtab,v,false,false,j,i);
				int w1=c.getPreferredSize().width;
				if (w<w1&&(wmax==0||w1<wmax)) w=w1;
			}
			w+=2;
			if (w<20) w=20;
			jtab.getColumnModel().getColumn(i).setPreferredWidth(w+2*margin);
		}
		jtab.repaint();
	}
}
