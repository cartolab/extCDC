package es.udc.cartolab.gvsig.algs.condcost;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.cresques.cts.IProjection;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.NoSuchTableException;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;
import com.iver.cit.gvsig.fmap.crs.CRSFactory;
import com.iver.cit.gvsig.fmap.edition.EditableAdapter;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.project.ProjectFactory;
import com.iver.cit.gvsig.project.documents.table.ProjectTable;
import com.jeta.forms.components.panel.FormPanel;

import es.udc.sextante.gridAnalysis.conditionalCost_alberto.AlbertoConditionalCostAlgorithm;
import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.WrongParameterIDException;
import es.unex.sextante.gvsig.core.gvOutputFactory;
import es.unex.sextante.gvsig.core.gvRasterLayer;
import es.unex.sextante.gvsig.core.gvTable;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.parameters.Parameter;

public class MovementMatrixPanel
         extends
            JPanel
         implements
            IWindow,
            ActionListener {

   public static final String       ID_MOVZONETB = "MovZoneTB";                         //javax.swing.JTable 
   public static final String       ID_MOVPANEL  = "MovPanel";                          //javax.swing.JEditorPane

   public static final String       ID_PREVIOUSB = "previousB";                         //javax.swing.JButton
   public static final String       ID_EXECUTEB  = "executeB";                          //javax.swing.JButton
   public static final String       ID_SAVEB     = "saveB";                             //javax.swing.JButton
   public static final String       ID_LOADB     = "loadB";                             //javax.swing.JButton


   private final FormPanel          formBody;

   private JTable                   movZoneTB;
   private JEditorPane              movPanel;

   private JButton                  loadB;
   private JButton                  saveB;

   private JButton                  previousB;
   private JButton                  executeB;

   WindowInfo                       viewInfo     = null;
   private String[]                 colNames     = null;

   private File                     MOV_ZONES_CSV;
   private File                     MOV_MATRIX_CSV;

   private final MovementZonesPanel MOV_ZONES_PANEL;

   private final String             path         = System.getProperty("java.io.tmpdir");


   public MovementMatrixPanel(final Object[] surfaceList,
                              final MovementZonesPanel movZonesPanel) {

      MOV_ZONES_PANEL = movZonesPanel;
      colNames = new String[surfaceList.length + 1];
      colNames[0] = "Zones";
      for (int i = 1; i < colNames.length; i++) {
         colNames[i] = surfaceList[i - 1].toString();
      }
      formBody = new FormPanel("MovementMatrixForm.xml");
      initWidgets();
      this.add(formBody);

   }


   private void resetTableModel() {
      if (movZoneTB != null) {
         movZoneTB.setModel(new DefaultTableModel(colNames, 0) {
            @Override
            public boolean isCellEditable(final int row,
                                          final int col) {
               if (col == 0) {
                  return false;
               }
               if ((row + 1) == col) {
                  return false;
               }
               return true;
            }


            @Override
            public Class getColumnClass(final int c) {
               return getValueAt(0, c).getClass();
            }
         });

      }
   }


   public void initTable() {

      resetTableModel();

      final TableColumnModel colModel = movZoneTB.getColumnModel();

      // Columns ...
      TableColumn auxColumn = movZoneTB.getColumnModel().getColumn(0);
      auxColumn.setMaxWidth(50);
      for (int i = 1; i < movZoneTB.getColumnCount(); i++) {
         final DefaultCellEditor chBoxEditor = new DefaultCellEditor(new JCheckBox());
         auxColumn = colModel.getColumn(i);
         auxColumn.setCellEditor(chBoxEditor);
         auxColumn.setMinWidth(20);
      }

      for (int i = 1; i < colNames.length; i++) {
         final Object[] row = new Object[movZoneTB.getColumnCount()];
         row[0] = new Integer(colNames[i]);
         for (int j = 1; j < movZoneTB.getColumnCount(); j++) {
            if (j == i) {
               row[j] = new Boolean(true);
            }
            else {
               row[j] = new Boolean(false);
            }
         }
         ((DefaultTableModel) movZoneTB.getModel()).addRow(row);
      }

      //		TableColumn zoneColumn = table.getColumnModel().getColumn(1);
      //		TableColumn costeColumn = table.getColumnModel().getColumn(4);
      //		nameColumn.setMinWidth(60);
      //		zoneColumn.setMaxWidth(50);
      //		costeColumn.setMinWidth(100);

   }


   public void initWidgets() {

      movZoneTB = (JTable) formBody.getComponentByName(ID_MOVZONETB);
      initTable();

      movPanel = (JEditorPane) formBody.getComponentByName(ID_MOVPANEL);

      //		addB = (JButton) formBody.getComponentByName(ID_ADDB);
      //		addB.addActionListener(this);
      //		removeB = (JButton) formBody.getComponentByName(ID_REMOVEB);
      //		removeB.addActionListener(this);
      loadB = (JButton) formBody.getComponentByName(ID_LOADB);
      loadB.addActionListener(this);
      saveB = (JButton) formBody.getComponentByName(ID_SAVEB);
      saveB.addActionListener(this);
      previousB = (JButton) formBody.getComponentByName(ID_PREVIOUSB);
      previousB.addActionListener(this);

      executeB = (JButton) formBody.getComponentByName(ID_EXECUTEB);
      executeB.addActionListener(this);

   }


   @Override
   public WindowInfo getWindowInfo() {

      if (viewInfo == null) {
         viewInfo = new WindowInfo(WindowInfo.MODELESSDIALOG | WindowInfo.RESIZABLE | WindowInfo.PALETTE);
         viewInfo.setTitle("Movement Matrix");
         viewInfo.setWidth(800);
         viewInfo.setHeight(400);
      }
      return viewInfo;
   }


   @Override
   public Object getWindowProfile() {
      return viewInfo.PROPERTIES_PROFILE;
   }


   //	private void callSextanteCondCostAlg() {
   //
   //		Sextante.initialize();
   //		String path;
   //		//logger.debug("Processing...  IRI_Clip_Grids.model");
   //		GeoAlgorithm alg = new ConditionalCostAlgorithm();
   //
   //		ParametersSet params = alg.getParameters();
   //
   //
   //		//DEFINE SEXTANTE INPUTS
   //		gvVectorLayer gvVertLyr = new gvVectorLayer();
   //		gvVertLyr.create(createOneElementLayer(wasteSpillFeat));
   //
   //		gvRasterLayer gvAccFlowLyr = new gvRasterLayer();
   //		gvAccFlowLyr.create(accFlowLayer);
   //		gvAccFlowLyr.setFullExtent();
   //		gvAccFlowLyr.open();
   //
   //		gvRasterLayer gvMDTLyr = new gvRasterLayer();
   //		gvMDTLyr.create(mdtLayer);
   //		gvMDTLyr.setFullExtent();
   //		gvMDTLyr.open();
   //
   //
   //		for (int i=0; i < params.getNumberOfParameters(); i++) {
   //			Parameter p = params.getParameter(i);
   //			if (p.getParameterDescription().equalsIgnoreCase("Vertido")){
   //				params.getParameter(i).setParameterValue(gvVertLyr);
   //			} else if (p.getParameterDescription().equalsIgnoreCase("MDT")) {
   //				params.getParameter(i).setParameterValue(gvMDTLyr);
   //			} else if (p.getParameterDescription().equalsIgnoreCase("AccFlow")) {
   //				params.getParameter(i).setParameterValue(gvAccFlowLyr);
   //			}
   //		}
   //
   //		//SET SEXTANTE OUTPUTS
   //
   //		try {
   //			buffer5kmFile= File.createTempFile("vertido5km", ".shp");
   //			clippedMDTFile= File.createTempFile("ClippedMDT", ".tif");
   //			buffer105mFile= File.createTempFile("vertido105m", ".shp");
   //			clippedAccFlowFile= File.createTempFile("ClippedAccFlow", ".tif");
   //		} catch (IOException e1) {
   //			//logger.debug("Error escribiendo en disco cositas Sextante Recorte Grids.");
   //			e1.printStackTrace();
   //			return;
   //		}
   //
   //		OutputObjectsSet outputs = alg.getOutputObjects();
   //		for (int i=0; i < outputs.getOutputLayersCount(); i++) {
   //			Output o = outputs.getOutput(i);
   //			if (o.getDescription().equalsIgnoreCase("vertido5km")){
   //				o.setOutputChannel(new FileOutputChannel(buffer5kmFile.getAbsolutePath()));
   //			} else if (o.getDescription().equalsIgnoreCase("ClippedMDT")){
   //				o.setOutputChannel(new FileOutputChannel(clippedMDTFile.getAbsolutePath()));
   //				//logger.debug("clippedMDTFile: " + clippedMDTFile.getAbsolutePath());
   //			} else if (o.getDescription().equalsIgnoreCase("vertido105m")){
   //				o.setOutputChannel(new FileOutputChannel(buffer105mFile.getAbsolutePath()));
   //			} else if (o.getDescription().equalsIgnoreCase("ClippedAccFlow")){
   //				//logger.debug("ClippedAccFlow: " + clippedAccFlowFile.getAbsolutePath());
   //				o.setOutputChannel(new FileOutputChannel(clippedAccFlowFile.getAbsolutePath()));
   //			} 
   //		}
   //
   //		try {
   //			OutputFactory outputFactory = new gvOutputFactory();
   //			alg.execute(null, outputFactory);
   //		} catch (GeoAlgorithmExecutionException e) {
   //			//logger.debug("ERROR: Error en el execute del Recorte de capas.");
   //			e.printStackTrace();
   //			return;
   //		}
   //
   //	}

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
                  System.out.println("ERROR!!!!!!!!!!!!!!!! Numero de columns incorrecto");
                  System.out.println("CSV: " + reader.getHeaders().length);
                  System.out.println("COL: " + colNames.length);
                  return;
               }

            }
            initTable();
            final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();
            model.setNumRows(0);

            int count = 0;
            while (reader.readRecord()) {
               final String[] csvValues = reader.getValues();
               final Object[] values = new Object[csvValues.length];
               for (int i = 0; i < csvValues.length; i++) {
                  if (i == 0) {
                     //Start at 1 because first column is ID
                     values[0] = csvValues[i];
                     continue;
                  }
                  values[i] = (csvValues[i].equalsIgnoreCase("true"));
               }
               model.insertRow(count, values);
               count++;
               System.out.println(count);
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
         final String[] ids = new String[model.getRowCount() + 1];
         ids[0] = "ZONES";
         for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < 1; j++) {
               final Object cellValue = model.getValueAt(i, j);
               final String value = cellValue.toString();
               ids[i + 1] = value;
            }
         }
         writer.writeRecord(ids);

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


   void executeConditionalCostAlg() {

      // PREPARE DATA

      //      final File costsFile = new File(path + "costs.tif");
      //      final File org_dstFile = new File(path + "org_dst.tif");
      //      //final File zdFile = new File(path + "ZD_esri_nacho.tif");
      //      final File zdFile = new File(path + "ZD_esri.tif");
      //
      //      final File ccs1File = new File(path + "ccs1_autopista.tif");
      //      final File ccs2File = new File(path + "ccs2_ferrocarril.tif");
      //

      final File movConstrainsFile = MOV_MATRIX_CSV;
      final File movSurfGroupsFile = MOV_ZONES_PANEL.MOV_ZONES_CSV;

      //TODO
      final IProjection projection = CRSFactory.getCRS("EPSG:23029");

      //      FLyrRasterSE costsLyr;
      //      FLyrRasterSE org_dstLyr;
      //      FLyrRasterSE zdLyr;
      //      FLyrRasterSE ccs1Lyr;
      //      FLyrRasterSE ccs2Lyr;
      IRasterLayer costsLyr;
      IRasterLayer orgDstLyr;
      IRasterLayer movZonesLyr;
      IRasterLayer ccs1Lyr;
      IRasterLayer ccs2Lyr;

      ProjectTable movConstraintsTable = null;
      ProjectTable movSurfGroupsTable = null;
      costsLyr = MOV_ZONES_PANEL.GLOBAL_COST_LYR;
      orgDstLyr = MOV_ZONES_PANEL.ORG_DST_LYR;
      movZonesLyr = MOV_ZONES_PANEL.MOV_ZONES_LYR;

      //TODO
      //TODO
      //TODO
      //TODO
      ccs1Lyr = MOV_ZONES_PANEL.COND_COST_LYRS[0];
      ccs2Lyr = MOV_ZONES_PANEL.COND_COST_LYRS[1];

      //Tables
      LayerFactory.getDataSourceFactory().addFileDataSource("csv string", "movement_constraints.csv",
               movConstrainsFile.getAbsolutePath());

      LayerFactory.getDataSourceFactory().addFileDataSource("csv string", "movement_surface_groups.csv",
               movSurfGroupsFile.getAbsolutePath());

      SelectableDataSource sds2;
      SelectableDataSource sds3;
      try {
         sds2 = new SelectableDataSource(LayerFactory.getDataSourceFactory().createRandomDataSource("movement_constraints.csv",
                  DataSourceFactory.AUTOMATIC_OPENING));
         //                          LayerFactory.getDataSourceFactory().createRandomDataSource("movement_surface_groups.csv",
         //                          DataSourceFactory.MANUAL_OPENING));
         final EditableAdapter ea2 = new EditableAdapter();
         ea2.setOriginalDataSource(sds2);
         movConstraintsTable = ProjectFactory.createTable("tabla2", ea2);

         sds3 = new SelectableDataSource(LayerFactory.getDataSourceFactory().createRandomDataSource(
                  "movement_surface_groups.csv", DataSourceFactory.AUTOMATIC_OPENING));

         final EditableAdapter ea3 = new EditableAdapter();
         ea3.setOriginalDataSource(sds3);
         movSurfGroupsTable = ProjectFactory.createTable("tabla3", ea3);
         //project.addTable(pt2);

      }
      catch (final ReadDriverException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      catch (final DriverLoadException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      catch (final NoSuchTableException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      final gvRasterLayer gvCOSTS = new gvRasterLayer();
      gvCOSTS.create(costsLyr);
      gvCOSTS.open();

      final gvRasterLayer gvORG_DST = new gvRasterLayer();
      gvORG_DST.create(orgDstLyr);
      gvORG_DST.open();

      final gvRasterLayer gvZD = new gvRasterLayer();
      gvZD.create(movZonesLyr);
      gvZD.open();

      final gvRasterLayer gvCCS1 = new gvRasterLayer();
      gvCCS1.create(ccs1Lyr);
      gvCCS1.open();

      final gvRasterLayer gvCCS2 = new gvRasterLayer();
      gvCCS2.create(ccs2Lyr);
      gvCCS2.open();

      final gvTable gvMovConstraints = new gvTable();
      gvMovConstraints.create(movConstraintsTable);

      final gvTable gvMovSurfGroups = new gvTable();
      gvMovSurfGroups.create(movSurfGroupsTable);

      //                gvTable gvMovConstraintsTable = new gvTable();
      //                gvMovConstraints.create(movConstraintsTable);


      // 

      Sextante.initialize();
      final AlbertoConditionalCostAlgorithm alg = new AlbertoConditionalCostAlgorithm();

      final ParametersSet params = alg.getParameters();


      Parameter costs_param;
      Parameter org_dst_param;
      Parameter zd_param;
      Parameter ccs_param;
      Parameter movConst_param;
      Parameter movSurfGroups_param;

      try {
         costs_param = params.getParameter(alg.COST);
         costs_param.setParameterValue(gvCOSTS);

         org_dst_param = params.getParameter(alg.ORIG_DEST);
         org_dst_param.setParameterValue(gvORG_DST);

         zd_param = params.getParameter(alg.MOVEMENT_SURFACES);
         zd_param.setParameterValue(gvZD);

         final ArrayList ccs_array = new ArrayList();
         ccs_array.add(gvCCS1);
         ccs_array.add(gvCCS2);

         ccs_param = params.getParameter(alg.CONDITIONAL_COST_SURFACES);
         ccs_param.setParameterValue(ccs_array);

         movConst_param = params.getParameter(alg.MOVEMENT_CONSTRAINTS_TABLE);
         movConst_param.setParameterValue(gvMovConstraints);

         movSurfGroups_param = params.getParameter(alg.MOVEMENT_SURFACES_GROUPS_TABLE);
         movSurfGroups_param.setParameterValue(gvMovSurfGroups);

         //     ccs1_param = params.getParameter(alg.C);
         //     css1_param.setParameterValue(gvZD);
         //
         //     zd_param = params.getParameter(alg.CONDITINAL_COST_SURFACES);
         //     zd_param.setParameterValue(gvZD);

      }
      catch (final WrongParameterIDException e1) {
         // TODO Auto-generated catch block
         e1.printStackTrace();
         return;
      }

      try {
         final OutputFactory outputFactory = new gvOutputFactory();

         final OutputObjectsSet outputs = alg.getOutputObjects();
         for (int i = 0; i < outputs.getOutputLayersCount(); i++) {
            final Output o = outputs.getOutput(i);
            if (o.getName().equalsIgnoreCase(alg.OUTPUT_ACCCOST)) {
               o.setOutputChannel(new FileOutputChannel(path + "output_acccost.tif"));
            }
            else if (o.getName().equalsIgnoreCase(alg.OUTPUT_CONDITIONAL_COSTS)) {
               o.setOutputChannel(new FileOutputChannel(path + "output_conditionalacccost_"));
            }
            else if (o.getName().equalsIgnoreCase(alg.OUTPUT_CLOSESTPOINT)) {
               o.setOutputChannel(new FileOutputChannel(path + "output_closestpoint_"));
            }
         }

         alg.execute(null, outputFactory);
      }
      catch (final GeoAlgorithmExecutionException e) {
         //logger.debug("ERROR: Error en el execute del Recorte de capas.");
         e.printStackTrace();
         return;
      }

      //                                (RasterDriver) LayerFactory.getDM().getDriver("gvSIG Image Driver"),
      //                                auxFile,
      //                                projection);

      //                mdtLayer.setCachingDrawnLayers(false);
      //
      //                gvRasterLayer gvAccFlowLyr = new gvRasterLayer();
      //                gvAccFlowLyr.create(accFlowLayer);
      //                gvAccFlowLyr.setFullExtent();
      //                gvAccFlowLyr.open();

   }


   @Override
   public void actionPerformed(final ActionEvent e) {

      final DefaultTableModel model = (DefaultTableModel) movZoneTB.getModel();
      if (e.getSource() == executeB) {
         try {
            MOV_MATRIX_CSV = File.createTempFile("CC_MOV_MATRIX_", ".csv");
            save2csv(MOV_MATRIX_CSV);
         }
         catch (final IOException e1) {
            e1.printStackTrace();
         }

         //prepareInfo();
         executeConditionalCostAlg();
      }

      if (e.getSource() == saveB) {
         save2csv();
      }

      if (e.getSource() == loadB) {
         loadCsv();
      }
   }

}
