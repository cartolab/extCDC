package es.udc.cartolab.gvsig.algs.condcost;

import java.io.File;
import java.util.ArrayList;

import org.cresques.cts.IProjection;
import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.NoSuchTableException;
import com.iver.andami.PluginServices;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.About;
import com.iver.cit.gvsig.exceptions.layers.LoadLayerException;
import com.iver.cit.gvsig.fmap.MapControl;
import com.iver.cit.gvsig.fmap.crs.CRSFactory;
import com.iver.cit.gvsig.fmap.edition.EditableAdapter;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.gui.panels.FPanelAbout;
import com.iver.cit.gvsig.project.ProjectFactory;
import com.iver.cit.gvsig.project.documents.table.ProjectTable;
import com.iver.cit.gvsig.project.documents.view.gui.BaseView;

import es.unex.sextante.core.OutputFactory;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.WrongParameterIDException;
import es.unex.sextante.gvsig.core.gvOutputFactory;
import es.unex.sextante.gvsig.core.gvRasterLayer;
import es.unex.sextante.gvsig.core.gvTable;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.parameters.Parameter;

public class ConditionalCostExtension
extends
Extension {

    String                     path       = "/home/nachouve/CARTOLAB/ALBERTO/old_DATA/";
    //TODO Proyeccion
    private static IProjection projection = CRSFactory.getCRS("EPSG:23029");


    public void execute(final String actionCommand) {

	// TODO Mejorar la interfaz de usuario. Deberi­a de componer la matriz a partir de las zonas que existan en el raster
	final BaseView view = (BaseView) PluginServices.getMDIManager().getActiveWindow();
	final MapControl mapControl = view.getMapControl();
	final ChooseGridsPanel cgp = new ChooseGridsPanel();
	cgp.setVisible(true);
	PluginServices.getMDIManager().addWindow(cgp);
	//executeConditionalCostAlg();

    }


    //TODO Get the layers from the GUI
    @Deprecated
    void executeTestConditionalCostAlg() {

	Sextante.initialize();
	final AlbertoConditionalCostAlgorithm alg = new AlbertoConditionalCostAlgorithm();

	final ParametersSet params = alg.getParameters();

	final File costsFile = new File(path + "costs.tif");
	final File org_dstFile = new File(path + "org_dst.tif");
	//final File zdFile = new File(path + "ZD_esri_nacho.tif");
	final File zdFile = new File(path + "ZD_esri.tif");

	final File ccs1File = new File(path + "ccs1_autopista.tif");
	final File ccs2File = new File(path + "ccs2_ferrocarril.tif");

	final File movConstrainsFile = new File(path + "movement_constraints.csv");
	final File movSurfGroupsFile = new File(path + "movement_surface_groups.csv");

	FLyrRasterSE costsLyr;
	FLyrRasterSE org_dstLyr;
	FLyrRasterSE zdLyr;
	FLyrRasterSE ccs1Lyr;
	FLyrRasterSE ccs2Lyr;

	ProjectTable movConstraintsTable = null;
	ProjectTable movSurfGroupsTable = null;
	try {

	    costsLyr = FLyrRasterSE.createLayer("COST", costsFile, projection);
	    org_dstLyr = FLyrRasterSE.createLayer("ORG_DST", org_dstFile, projection);
	    zdLyr = FLyrRasterSE.createLayer("ZD", zdFile, projection);
	    ccs1Lyr = FLyrRasterSE.createLayer("ccs1", ccs1File, projection);
	    ccs2Lyr = FLyrRasterSE.createLayer("ccs2", ccs2File, projection);

	    //Tables
	    LayerFactory.getDataSourceFactory().addFileDataSource("csv string", "movement_constraints.csv",
		    movConstrainsFile.getAbsolutePath());

	    LayerFactory.getDataSourceFactory().addFileDataSource("csv string", "movement_surface_groups.csv",
		    movSurfGroupsFile.getAbsolutePath());

	    SelectableDataSource sds2;
	    SelectableDataSource sds3;
	    try {
		sds2 = new SelectableDataSource(LayerFactory.getDataSourceFactory().createRandomDataSource(
			"movement_constraints.csv", DataSourceFactory.AUTOMATIC_OPENING));
		//				LayerFactory.getDataSourceFactory().createRandomDataSource("movement_surface_groups.csv",
		//				DataSourceFactory.MANUAL_OPENING));
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

	    //			Project p = ((ProjectExtension) PluginServices.getExtension(ProjectExtension.class)).getProject();
	    //			ArrayList<ProjectDocument> docs = p.getDocuments();
	    //			for (int i = 0; i < docs.size(); i++){
	    //				ProjectDocument doc = docs.get(i);
	    //			//File movSurfGroupsFile = new File(path + "movement_surface_groups.csv");
	    //				if (doc.getName().equalsIgnoreCase("movement_constraints.csv")){
	    //					System.out.println(doc.getName() + "  "+ doc.getProject());
	    //
	    //				}
	    //
	    //			}

	}
	catch (final LoadLayerException e2) {
	    // TODO Auto-generated catch block
	    e2.printStackTrace();
	    return;
	}

	final gvRasterLayer gvCOSTS = new gvRasterLayer();
	gvCOSTS.create(costsLyr);
	gvCOSTS.open();

	final gvRasterLayer gvORG_DST = new gvRasterLayer();
	gvORG_DST.create(org_dstLyr);
	gvORG_DST.open();

	final gvRasterLayer gvZD = new gvRasterLayer();
	gvZD.create(zdLyr);
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

	//		gvTable gvMovConstraintsTable = new gvTable();
	//		gvMovConstraints.create(movConstraintsTable);

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

	    //	ccs1_param = params.getParameter(alg.C);
	    //	css1_param.setParameterValue(gvZD);
	    //
	    //	zd_param = params.getParameter(alg.CONDITINAL_COST_SURFACES);
	    //	zd_param.setParameterValue(gvZD);

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

	//				(RasterDriver) LayerFactory.getDM().getDriver("gvSIG Image Driver"),
	//				auxFile,
	//				projection);

	//		mdtLayer.setCachingDrawnLayers(false);
	//
	//		gvRasterLayer gvAccFlowLyr = new gvRasterLayer();
	//		gvAccFlowLyr.create(accFlowLayer);
	//		gvAccFlowLyr.setFullExtent();
	//		gvAccFlowLyr.open();

    }


    public void initialize() {
	final About about = (About) PluginServices.getExtension(About.class);
	final FPanelAbout panelAbout = about.getAboutPanel();
	final java.net.URL aboutURL = this.getClass().getResource("/about.htm");
	panelAbout.addAboutUrl("Conditional Cost", aboutURL);
    }


    public boolean isEnabled() {
	return true;
    }


    public boolean isVisible() {
	final com.iver.andami.ui.mdiManager.IWindow f = PluginServices.getMDIManager().getActiveWindow();
	if (f == null) {
	    return false;
	}
	if (f instanceof BaseView) {
	    return true;
	}
	return false;
    }

}
