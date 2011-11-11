package es.udc.cartolab.gvsig.algs.condcost;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

public class ComboBoxCellEditor extends DefaultCellEditor{ 
	public ComboBoxCellEditor(String[] items) {
//		JComboBox cbox = new JComboBox(items);
//		cbox.setEditable(true);
//		cbox.setSize(25, 100);
		super(new JComboBox(items));
	} 	
} 

