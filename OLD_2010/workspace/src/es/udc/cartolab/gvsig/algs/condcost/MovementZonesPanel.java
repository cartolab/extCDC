package es.udc.cartolab.gvsig.algs.condcost;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;
import com.jeta.forms.components.panel.FormPanel;

import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.gui.core.SextanteGUI;

public class MovementZonesPanel
         extends
            JPanel
         implements
            IWindow,
            ActionListener {

   public static final String ID_MOVZONETB = "MovZoneTB";                                                                   //javax.swing.JTable
   public static final String ID_MOVPANEL  = "MovPanel";                                                                    //javax.swing.JEditorPane

   public static final String ID_ADDB      = "addB";                                                                        //javax.swing.JButton
   public static final String ID_REMOVEB   = "removeB";                                                                     //javax.swing.JButton
   public static final String ID_SAVEB     = "saveB";                                                                       //javax.swing.JButton
   public static final String ID_NEXTB     = "nextB";                                                                       //javax.swing.JButton
   public static final String ID_LOADB     = "loadB";                                                                       //javax.swing.JButton

   private final FormPanel    formBody;

   private JTable             movZoneTB;
   private JEditorPane        movPanel;

   private JButton            addB;
   private JButton            removeB;
   private JButton            nextB;
   private JButton            loadB;
   private JButton            saveB;

   WindowInfo                 viewInfo     = null;

   private final String[]     colNames     = { "Zone", "Name", "Group", "Cond_Cost", "Link", "CondCostGrid", "Depends_On" };

   //String[] layerItems = new String[]{"A", "B", "C"};
   //ObjectAndDescription[]     rastersOAD   = null;
   //ObjectAndDescription[]     rastersOAD2  = null;
   String[]                   ccsLyrs      = null;
   String[]                   groupItems   = new String[] { "--", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
   Object[]                   surfacesList = null;

   //TODO Set those variables as PRIVATE and create get/set methods 
   IRasterLayer               GLOBAL_COST_LYR;
   IRasterLayer               ORG_DST_LYR;
   IRasterLayer               MOV_ZONES_LYR;
   IRasterLayer[]             COND_COST_LYRS;

   File                       MOV_ZONES_CSV;


   public MovementZonesPanel(final String globalCostLyrName,
                             final String orgDstLyrName,
                             final String movDefLyrName,
                             final String[] condCostLyrNames) {

      //Get Raster layers from gvSIG
      SextanteGUI.getInputFactory().createDataObjects();
      final IRasterLayer[] rasters = SextanteGUI.getInputFactory().getRasterLayers();

      // Get the layers selected on the ToC
      int ccLyrsCounter = 0;
      if ((condCostLyrNames != null) && (condCostLyrNames.length > 0)) {
         COND_COST_LYRS = new IRasterLayer[condCostLyrNames.length];
         for (final IRasterLayer raster : rasters) {
            for (final String condCostLyrName : condCostLyrNames) {
               if (condCostLyrName.equalsIgnoreCase(raster.getName())) {
                  COND_COST_LYRS[ccLyrsCounter] = raster;
                  ccLyrsCounter++;
               }
            }
         }
      }
      for (final IRasterLayer raster : rasters) {
         if (globalCostLyrName.equalsIgnoreCase(raster.getName())) {
            GLOBAL_COST_LYR = raster;
         }
         if (orgDstLyrName.equalsIgnoreCase(raster.getName())) {
            ORG_DST_LYR = raster;
         }
         if (movDefLyrName.equalsIgnoreCase(raster.getName())) {
            MOV_ZONES_LYR = raster;
         }
      }

      //TODO Get all values of the movDefLyr as Z_Class of the table
      try {
         MOV_ZONES_LYR.postProcess();
      }
      catch (final Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      MOV_ZONES_LYR.setFullExtent();
      MOV_ZONES_LYR.open();

      final int m_iNX = MOV_ZONES_LYR.getNX();
      final int m_iNY = MOV_ZONES_LYR.getNY();

      final int counter = 0;
      final HashSet<Integer> surfacesID = new HashSet<Integer>();
      for (int y = 0; y < m_iNY; y++) {
         for (int x = 0; x < m_iNX; x++) {
            final int value = MOV_ZONES_LYR.getCellValueAsInt(x, y);
            //                          if (value == ORG_DST_DATA){
            //                                  continue;
            //                          }
            if (!MOV_ZONES_LYR.isNoDataValue(value) && !surfacesID.contains(value)) {
               surfacesID.add(value);
            }
         }
      }
      surfacesList = surfacesID.toArray();
      Arrays.sort(surfacesList);

      int num_ccs = 0;
      if (condCostLyrNames != null) {
         num_ccs = condCostLyrNames.length;
      }
      ccsLyrs = new String[num_ccs + 1];
      ccsLyrs[0] = "--";
      for (int i = 0; i < num_ccs; i++) {
         ccsLyrs[i + 1] = condCostLyrNames[i];
      }

      formBody = new FormPanel("MovementZonesForm.xml");
      initWidgets();


      final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();
      for (final Object surfaceID : surfacesList) {
         //System.out.println("rastersOAD: " + element.getDescription());
         model.insertRow(model.getRowCount(), new Object[] { surfaceID, "", "--", new Boolean(false), new Boolean(false),
                  new Boolean(false), ccsLyrs[0] });
      }

      this.add(formBody);

   }


   public void initTable() {

      movZoneTB.setModel(new DefaultTableModel(colNames, 0) {
         @Override
         public boolean isCellEditable(final int row,
                                       final int col) {
            return true;
         }


         @Override
         public Class getColumnClass(final int c) {
            return getValueAt(0, c).getClass();
         }
      });

      final JTextField namesTF = new JTextField();
      final JComboBox dependsOnCbox = new JComboBox(ccsLyrs);
      final JComboBox groupsCbox = new JComboBox(groupItems);

      final CBTableCellRenderer renderer = new CBTableCellRenderer();

      groupsCbox.setRenderer(renderer);

      final TableCellEditor namesEditor = new DefaultCellEditor(namesTF);
      final TableCellEditor groupEditor = new DefaultCellEditor(groupsCbox);
      final TableCellEditor dependsOnEditor = new DefaultCellEditor(dependsOnCbox);

      // Column Zone
      final TableColumn zoneColumn = movZoneTB.getColumnModel().getColumn(0);
      zoneColumn.setCellEditor(namesEditor);
      zoneColumn.setCellRenderer(renderer);

      // Column NAME
      final TableColumn nameColumn = movZoneTB.getColumnModel().getColumn(1);
      //      nameColumn.setCellEditor(namesEditor);
      nameColumn.setCellRenderer(renderer);

      // Column GROUP
      final TableColumn groupColumn = movZoneTB.getColumnModel().getColumn(2);
      groupColumn.setCellEditor(groupEditor);
      groupColumn.setCellRenderer(renderer);

      // Columns ...
      for (int i = 3; i < 5; i++) {
         final DefaultCellEditor chBoxEditor = new DefaultCellEditor(new JCheckBox());
         final TableColumn auxColumn = movZoneTB.getColumnModel().getColumn(i);
         auxColumn.setCellEditor(chBoxEditor);
         //auxColumn.setCellRenderer(renderer);
      }

      // Column DEPENDS_ON
      final TableColumn dependsOnColumn = movZoneTB.getColumnModel().getColumn(6);
      dependsOnColumn.setCellEditor(dependsOnEditor);
      dependsOnColumn.setCellRenderer(renderer);

      //		TableColumn zoneColumn = table.getColumnModel().getColumn(1);
      //		TableColumn costeColumn = table.getColumnModel().getColumn(4);
      final int SHORT = 45;
      final int LONG = 120;
      zoneColumn.setMaxWidth(SHORT);
      nameColumn.setMinWidth(LONG);
      groupColumn.setMaxWidth(SHORT);
      dependsOnColumn.setMinWidth(LONG + 40);
      //		zoneColumn.setMaxWidth(50);
      //		costeColumn.setMinWidth(100);

   }


   public void initWidgets() {
      movZoneTB = (JTable) formBody.getComponentByName(ID_MOVZONETB);
      initTable();

      movPanel = (JEditorPane) formBody.getComponentByName(ID_MOVPANEL);

      addB = (JButton) formBody.getComponentByName(ID_ADDB);
      addB.addActionListener(this);
      addB.setEnabled(false);
      removeB = (JButton) formBody.getComponentByName(ID_REMOVEB);
      removeB.addActionListener(this);
      removeB.setEnabled(false);
      loadB = (JButton) formBody.getComponentByName(ID_LOADB);
      loadB.addActionListener(this);
      saveB = (JButton) formBody.getComponentByName(ID_SAVEB);
      saveB.addActionListener(this);
      nextB = (JButton) formBody.getComponentByName(ID_NEXTB);
      nextB.addActionListener(this);

   }


   @Override
   public WindowInfo getWindowInfo() {

      if (viewInfo == null) {
         viewInfo = new WindowInfo(WindowInfo.MODELESSDIALOG | WindowInfo.RESIZABLE | WindowInfo.PALETTE);
         viewInfo.setTitle("Conditional Cost");
         viewInfo.setWidth(800);
         viewInfo.setHeight(400);
      }
      return viewInfo;
   }


   public Object getWindowProfile() {
      return viewInfo.PROPERTIES_PROFILE;
   }


   private void loadCsv() {

      final JFileChooser chooser = new JFileChooser();
      final int resp = chooser.showOpenDialog(this);
      try {
         if (resp == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            final CsvReader reader = new CsvReader(file.getAbsolutePath());
            reader.setDelimiter(';');
            if (reader.readHeaders()) {
               //Suponemos que son los headers correctos
               if (reader.getHeaders().length != colNames.length) {
                  System.out.println("ERROR!!!!!!!!!!!!!!!!");
                  return;
               }

            }

            final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();
            model.setRowCount(0);

            int count = 0;
            while (reader.readRecord()) {
               model.insertRow(count, reader.getValues());
               count++;
            }
            reader.close();
         }

      }
      catch (final IOException e) {
         e.printStackTrace();
      }
   }


   private void save2csv(final File file) {

      final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();
      try {
         final CsvWriter writer = new CsvWriter(file.getAbsolutePath());
         writer.setDelimiter(';');

         //Get HEADERS 
         writer.writeRecord(colNames);

         //Get VALUES
         for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
               final Object cellValue = model.getValueAt(i, j);
               final String value = cellValue.toString();
               writer.write(value);
            }
            writer.endRecord();
         }
         writer.close();
      }
      catch (final IOException e) {
         e.printStackTrace();
      }
   }


   private void save2csv() {
      final JFileChooser chooser = new JFileChooser();
      final int resp = chooser.showSaveDialog(this);

      if (resp == JFileChooser.APPROVE_OPTION) {
         final File file = chooser.getSelectedFile();
         save2csv(file);
      }
   }


   public void actionPerformed(final ActionEvent e) {

      final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();

      if (e.getSource() == nextB) {

         try {
            MOV_ZONES_CSV = File.createTempFile("CC_MOV_ZONES_", ".csv");
            save2csv(MOV_ZONES_CSV);
         }
         catch (final IOException e1) {
            e1.printStackTrace();
         }

         //this.setVisible(false);
         final MovementMatrixPanel mmp = new MovementMatrixPanel(surfacesList, this);
         //mmp.setVisible(true);
         PluginServices.getMDIManager().addWindow(mmp);

      }

      //      if (e.getSource() == addB) {
      //         model.addRow(new Object[] { rastersOAD[0], "A", new Boolean(false), new Boolean(true), new Boolean(false),
      //                  rastersOAD2[0] });
      //      }

      if (e.getSource() == removeB) {

         if (movZoneTB.getCellEditor() != null) {
            movZoneTB.getCellEditor().cancelCellEditing();
         }
         int rowsCount = movZoneTB.getRowCount();
         int rowSelected = movZoneTB.getSelectedRow();

         System.out.println("Remove line!!! selected: " + rowSelected + "  count: " + rowsCount);

         if (rowSelected > -1) {
            model.removeRow(rowSelected);
            model.fireTableRowsDeleted(rowSelected, rowSelected);
            movZoneTB.repaint();
            movZoneTB.revalidate();

            // Select one of the others rows
            rowsCount = movZoneTB.getRowCount();
            if (rowsCount > 0) {
               //rowSelected -= 1;
               if (rowsCount <= rowSelected) {
                  rowSelected = rowsCount - 1;
               }
               if (rowSelected > -1) {
                  final ListSelectionModel selectionModel = movZoneTB.getSelectionModel();
                  selectionModel.setSelectionInterval(rowSelected, rowSelected);
               }
            }
            else if (rowsCount == 0) {
               this.setVisible(false);
               this.setVisible(true);
            }
            rowsCount = movZoneTB.getRowCount();
            rowSelected = movZoneTB.getSelectedRow();
            System.out.println("Done!!! selected: " + rowSelected + "  count: " + rowsCount);
         }

      }

      if (e.getSource() == saveB) {
         save2csv();
      }

      if (e.getSource() == loadB) {
         loadCsv();
      }

   }

}
