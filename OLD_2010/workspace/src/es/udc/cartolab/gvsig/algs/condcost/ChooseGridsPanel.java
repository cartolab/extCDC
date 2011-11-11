package es.udc.cartolab.gvsig.algs.condcost;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;
import com.jeta.forms.components.panel.FormPanel;

import es.unex.sextante.core.ObjectAndDescription;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.gui.core.SextanteGUI;

public class ChooseGridsPanel
         extends
            JPanel
         implements
            IWindow,
            ActionListener,
            ItemListener {

   public static final String ID_MOVPANEL         = "MovPanel";        //javax.swing.JEditorPane
   public static final String ID_NEXTB            = "nextB";           //javax.swing.JButton
   public static final String ID_GLOBALCOSTCB     = "globalCostCB";    //javax.swing.JComboBox
   public static final String ID_ORGDSTCB         = "orgDstCB";        //javax.swing.JComboBox
   public static final String ID_ZONESDEFCB       = "zonesDefCB";      //javax.swing.JComboBox
   public static final String ID_CONDITINALCOSTTB = "ConditinalCostTB"; //javax.swing.JTable
   public static final String ID_ADDB             = "addB";            //javax.swing.JButton
   public static final String ID_REMOVEB          = "removeB";         //javax.swing.JButton
   public static final String ID_LOADB            = "loadB";           //javax.swing.JButton
   public static final String ID_SAVEB            = "saveB";           //javax.swing.JButton

   private final FormPanel    formBody;

   private JTable             condCostTB;
   private JEditorPane        movPanel;

   private JComboBox          globalCostCB;
   private JComboBox          orgDstCB;
   private JComboBox          zonesDefCB;

   private JButton            nextB;
   private JButton            addB;
   private JButton            removeB;

   WindowInfo                 viewInfo            = null;

   private String[]           colNames            = null;

   ObjectAndDescription[]     rastersOAD          = null;
   ObjectAndDescription[]     rastersOAD2         = null;


   public ChooseGridsPanel() {

      //Get Raster layers from gvSIG
      SextanteGUI.getInputFactory().createDataObjects();

      final IRasterLayer[] rasters = SextanteGUI.getInputFactory().getRasterLayers();
      rastersOAD = new ObjectAndDescription[rasters.length];
      rastersOAD2 = new ObjectAndDescription[rasters.length + 1];
      rastersOAD2[0] = new ObjectAndDescription("--", "--");
      for (int i = 0; i < rasters.length; i++) {
         rastersOAD[i] = new ObjectAndDescription(rasters[i].getName(), rasters[i]);
         rastersOAD2[i + 1] = new ObjectAndDescription(rasters[i].getName(), rasters[i]);
      }

      colNames = new String[1];
      colNames[0] = "Grid Name";
      //		for (int i = 1; i <= NumberOfConditionalCosts; i++){
      //			colNames[i] = String.valueOf(i);
      //		}
      formBody = new FormPanel("ChooseGridsForm.xml");
      initWidgets();
      this.add(formBody);

   }


   public void initComboBox(final JComboBox cb,
                            final boolean selected) {
      //Stop listen events
      cb.removeItemListener(this);

      final ArrayList excludeList = new ArrayList();
      if (cb != globalCostCB) {
         excludeList.add(globalCostCB.getSelectedItem());
      }
      if (cb != orgDstCB) {
         excludeList.add(orgDstCB.getSelectedItem());
      }
      if (cb != zonesDefCB) {
         excludeList.add(zonesDefCB.getSelectedItem());
      }

      // TODO It's not needed remove and add all!!!!
      final String selectedLayer = (String) cb.getSelectedItem();
      if (selected) {
         excludeList.remove(selectedLayer);
      }
      cb.removeAllItems();

      for (final ObjectAndDescription element : rastersOAD) {
         final String layer = element.getDescription();
         //if (!excludeList.contains(layer))
         cb.addItem(layer);
      }
      if ((selectedLayer != null) && !excludeList.contains(selectedLayer)) {
         cb.setSelectedItem(selectedLayer);
      }
      else {
         for (final ObjectAndDescription element : rastersOAD) {
            final String layer = element.getDescription();
            if (!excludeList.contains(layer)) {
               cb.setSelectedItem(layer);
               break;
            }
         }
      }
      //Start listen events again
      cb.addItemListener(this);
   }


   public void initTable() {

      condCostTB.setModel(new DefaultTableModel(colNames, 0) {
         @Override
         public boolean isCellEditable(final int row,
                                       final int col) {
            //				if (col == 0){
            //					return false;
            //				}
            //				if ((row+1) == col){
            //					return false;
            //				}
            return false;
         }


         @Override
         public Class getColumnClass(final int c) {
            return getValueAt(0, c).getClass();
         }

      });

      System.out.println("COLUMNAAAAAAAAAAAAAAAAAAAS: " + condCostTB.getModel().getColumnCount());

      final TableColumnModel colModel = condCostTB.getColumnModel();

      // Columns ...
      TableColumn auxColumn = condCostTB.getColumnModel().getColumn(0);
      //auxColumn.setMaxWidth(50);
      for (int i = 1; i < condCostTB.getColumnCount(); i++) {
         final DefaultCellEditor chBoxEditor = new DefaultCellEditor(new JCheckBox());
         auxColumn = colModel.getColumn(i);
         auxColumn.setCellEditor(chBoxEditor);
         auxColumn.setMinWidth(20);
      }

      for (int i = 1; i < condCostTB.getColumnCount(); i++) {
         final Object[] row = new Object[condCostTB.getColumnCount()];
         row[0] = new Integer(i);
         for (int j = 1; j < condCostTB.getColumnCount(); j++) {
            if (j == i) {
               row[j] = new Boolean(true);
            }
            else {
               row[j] = new Boolean(false);
            }
         }
         ((DefaultTableModel) condCostTB.getModel()).addRow(row);
      }

      //		TableColumn zoneColumn = table.getColumnModel().getColumn(1);
      //		TableColumn costeColumn = table.getColumnModel().getColumn(4);
      //		nameColumn.setMinWidth(60);
      //		zoneColumn.setMaxWidth(50);
      //		costeColumn.setMinWidth(100);

   }


   public void initWidgets() {
      condCostTB = (JTable) formBody.getComponentByName(ID_CONDITINALCOSTTB);
      initTable();

      movPanel = (JEditorPane) formBody.getComponentByName(ID_MOVPANEL);

      addB = (JButton) formBody.getComponentByName(ID_ADDB);
      addB.addActionListener(this);
      removeB = (JButton) formBody.getComponentByName(ID_REMOVEB);
      removeB.addActionListener(this);
      //		loadB = (JButton) formBody.getComponentByName(ID_LOADB);
      //		loadB.addActionListener(this);
      //		saveB = (JButton) formBody.getComponentByName(ID_SAVEB);
      //		saveB.addActionListener(this);
      nextB = (JButton) formBody.getComponentByName(ID_NEXTB);
      nextB.addActionListener(this);

      globalCostCB = (JComboBox) formBody.getComponentByName(ID_GLOBALCOSTCB);
      globalCostCB.addItemListener(this);

      orgDstCB = (JComboBox) formBody.getComponentByName(ID_ORGDSTCB);
      orgDstCB.addItemListener(this);

      zonesDefCB = (JComboBox) formBody.getComponentByName(ID_ZONESDEFCB);
      zonesDefCB.addItemListener(this);

      initComboBox(globalCostCB, false);
      initComboBox(orgDstCB, false);
      initComboBox(zonesDefCB, false);

   }


   @Override
   public WindowInfo getWindowInfo() {

      if (viewInfo == null) {
         viewInfo = new WindowInfo(WindowInfo.MODELESSDIALOG | WindowInfo.RESIZABLE | WindowInfo.PALETTE);
         viewInfo.setTitle("Conditional Costs Inputs");
         viewInfo.setWidth(800);
         viewInfo.setHeight(400);
      }
      return viewInfo;
   }


   @Override
   public Object getWindowProfile() {
      return viewInfo.PROPERTIES_PROFILE;
   }


   @Override
   public void actionPerformed(final ActionEvent e) {

      final DefaultTableModel model = (DefaultTableModel) condCostTB.getModel();

      if (e.getSource() == addB) {
         final JFrame f = new JFrame("Choose a Conditional Cost Grid");
         final JPanel p = new JPanel();
         p.setLayout(new BorderLayout());
         final JLabel label = new JLabel("Conditional Cost Grid: ");
         p.add(label, BorderLayout.WEST);
         final JComboBox cb = new JComboBox();
         //TODO no deberi­a tener los ya escogidos
         initComboBox(cb, false);
         p.add(cb, BorderLayout.CENTER);
         final JButton b = new JButton("Ok");
         b.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
               model.addRow(new String[] { (String) cb.getSelectedItem() });
               f.setVisible(false);
            }
         });
         p.add(b, BorderLayout.SOUTH);
         f.add(p);
         f.setSize(400, 300);
         f.setVisible(true);

      }

      if (e.getSource() == removeB) {

         if (condCostTB.getCellEditor() != null) {
            condCostTB.getCellEditor().cancelCellEditing();
         }
         int rowsCount = condCostTB.getRowCount();
         int rowSelected = condCostTB.getSelectedRow();

         System.out.println("Remove line!!! selected: " + rowSelected + "  count: " + rowsCount);
         if (rowSelected > -1) {
            model.removeRow(rowSelected);
            model.fireTableRowsDeleted(rowSelected, rowSelected);
            condCostTB.repaint();
            condCostTB.revalidate();

            // Select one of the others rows
            rowsCount = condCostTB.getRowCount();
            if (rowsCount > 0) {
               //rowSelected -= 1;
               if (rowsCount <= rowSelected) {
                  rowSelected = rowsCount - 1;
               }
               if (rowSelected > -1) {
                  final ListSelectionModel selectionModel = condCostTB.getSelectionModel();
                  selectionModel.setSelectionInterval(rowSelected, rowSelected);
               }
            }
            else if (rowsCount == 0) {
               this.setVisible(false);
               this.setVisible(true);
            }
            rowsCount = condCostTB.getRowCount();
            rowSelected = condCostTB.getSelectedRow();
            System.out.println("Done!!! selected: " + rowSelected + "  count: " + rowsCount);
         }

      }

      if (e.getSource() == nextB) {
         //this.setVisible(false);

         final MovementZonesPanel mzp = new MovementZonesPanel((String) globalCostCB.getSelectedItem(),
                  (String) orgDstCB.getSelectedItem(), (String) zonesDefCB.getSelectedItem(), getCondCostLayers());
         mzp.setVisible(true);

         PluginServices.getMDIManager().addWindow(mzp);
      }

   }


   private String[] getCondCostLayers() {
      if (condCostTB.getRowCount() == 0) {
         return null;
      }
      final String[] lyrs = new String[condCostTB.getRowCount()];
      final DefaultTableModel model = (DefaultTableModel) condCostTB.getModel();
      for (int i = 0; i < condCostTB.getRowCount(); i++) {
         lyrs[i] = (String) model.getValueAt(i, 0);
      }
      return lyrs;
   }


   @Override
   public void itemStateChanged(final ItemEvent e) {

      if (e.getSource() instanceof JComboBox) {
         initComboBox((JComboBox) e.getSource(), true);
         if (e.getSource() == globalCostCB) {
            initComboBox(orgDstCB, false);
            initComboBox(zonesDefCB, false);
         }
         if (e.getSource() == orgDstCB) {
            initComboBox(globalCostCB, false);
            initComboBox(zonesDefCB, false);
         }
         if (e.getSource() == zonesDefCB) {
            initComboBox(globalCostCB, false);
            initComboBox(orgDstCB, false);
         }
      }

   }

}
