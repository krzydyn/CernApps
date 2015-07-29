package meas.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import meas.ChannelDef;
import pl.common.ui.DialogPanel;

@SuppressWarnings("serial")
public class ChannelSelector extends DialogPanel {
	private final List<ChannelDef> chl;
	private final JList<String> chList;
	public ChannelSelector(List<ChannelDef> vp,List<ChannelDef> cur){
		super(new GridBagLayout());
		if (cur!=null && cur.size()>0) {
			chl=new ArrayList<ChannelDef>();
			for (ChannelDef c:vp) {
				if (cur.contains(c)) continue;
				if (c.unit.equals(cur.get(0).unit))// && c.name.startsWith(filt.name.substring(0,2)))
					chl.add(c);
			}
		}
		else chl=vp;
		vp=null;
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.BOTH;
		constr.weightx=constr.weighty=1;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		DefaultListModel<String> lm=new DefaultListModel<String>();
		JList<String> l=new JList<String>(lm);
		l.setBackground(UIManager.getColor("MenuItem.background"));
		l.setFont(UIManager.getFont("MenuItem.font"));
		for (int i=0; i<chl.size(); ++i){
			ChannelDef pd=chl.get(i);
			lm.add(i,String.format("%s[%d] (%s)",pd.name,pd.id,pd.descr));
		}
		JScrollPane sp=new JScrollPane(l,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(sp,constr);
		l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chList=l;
		addButtons();
		l.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent ev){
				if (ev.getButton()==MouseEvent.BUTTON1 &&
						ev.getClickCount()==2)
					if (defaultButton!=null) defaultButton.doClick();
			}
		});
		//log.debug("jlist pref=%s",chList.getPreferredSize().toString());
		//log.debug("sp pref=%s",sp.getPreferredSize().toString());
		Dimension d=sp.getPreferredSize();
		d.height=Math.min(chl.size()*18+10,500);
		sp.setPreferredSize(d);
	}
	public int getItems() {
		return chl.size();
	}
	public void setSelectedItem(String name){
		for (int i=0; i<chl.size(); ++i){
			if (name.equals(chl.get(i).name)){
				chList.setSelectedIndex(i);
				chList.scrollRectToVisible(chList.getCellBounds(i,i+1));
				break;
			}
		}
	}
	public ChannelDef getSelectedItem(){
		return chl.get(chList.getSelectedIndex());
	}
}
