package sys.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;

import sys.Logger;

@SuppressWarnings("serial")
public class CommandAction extends AbstractAction {
	Logger log=Logger.getLogger();
	final ActionListener listener;
	final ActionEvent ev;
	public CommandAction(String cmd,ActionListener l){
		super();
		listener=l;
		ev=new ActionEvent(l,0,cmd);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		ev.setSource(e.getSource());
		listener.actionPerformed(ev);
	}
}
