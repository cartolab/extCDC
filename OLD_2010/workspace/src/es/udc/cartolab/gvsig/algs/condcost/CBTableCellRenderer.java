package es.udc.cartolab.gvsig.algs.condcost;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTable;

import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class CBTableCellRenderer implements ListCellRenderer, TableCellRenderer {

	DefaultListCellRenderer listRenderer = new DefaultListCellRenderer();
	DefaultTableCellRenderer tableRenderer = new DefaultTableCellRenderer();

//	private void configureRenderer(JLabel renderer, Object value) {
//		renderer.setText((String) value.toString());
//	}

	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		listRenderer = (DefaultListCellRenderer) listRenderer.getListCellRendererComponent(list, value,
				index, isSelected, cellHasFocus);
		//configureRenderer(listRenderer, value);
		return listRenderer;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		tableRenderer = (DefaultTableCellRenderer) tableRenderer.getTableCellRendererComponent(table,
				value, isSelected, hasFocus, row, column);
		//configureRenderer(tableRenderer, value);
		return tableRenderer;
	}
}
