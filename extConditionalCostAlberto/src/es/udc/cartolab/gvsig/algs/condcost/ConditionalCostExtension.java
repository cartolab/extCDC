package es.udc.cartolab.gvsig.algs.condcost;

import java.io.File;

import org.cresques.cts.IProjection;
import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.NoSuchTableException;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.exceptions.layers.LoadLayerException;
import com.iver.cit.gvsig.fmap.crs.CRSFactory;
import com.iver.cit.gvsig.fmap.edition.EditableAdapter;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;
import com.iver.cit.gvsig.project.ProjectFactory;
import com.iver.cit.gvsig.project.documents.table.ProjectTable;

import es.udc.sextante.gridAnalysis.conditionalCost_alberto.AlbertoConditionalCostAlgorithm;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;

public class ConditionalCostExtension extends Extension {

	//TODO Proyeccion
	private static IProjection projection = CRSFactory.getCRS("EPSG:23029");
	private static String path = "/home/nachouve/CARTOLAB/ALBERTO/DATA/";

	public void execute(String actionCommand) {
		// TODO Launch algorithm GUI (jlopez)
		// final BaseView view = (BaseView) PluginServices.getMDIManager().getActiveWindow();
		// executeConditionalCostAlg();
		executeTest();
	}

	private void executeTest() {
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


		} catch (LoadLayerException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		} catch (ReadDriverException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DriverLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchTableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public void initialize() {
		//Nothing
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean isVisible() {
		return true;
	}

}
