package es.udc.cartolab.gvsig.algs.condcost;

import org.cresques.cts.IProjection;

import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.fmap.crs.CRSFactory;

import es.udc.sextante.gridAnalysis.conditionalCost_alberto.AlbertoConditionalCostAlgorithm;
import es.unex.sextante.core.Sextante;

/**
 * Extension to devel the GUI
 * * We will merge both extensions in the future... :)
 *
 */
public class GUIConditionalCostExtension extends Extension {

    //TODO Proyeccion 
    private static IProjection projection = CRSFactory.getCRS("EPSG:23029");

    public void execute(String actionCommand) {
	// TODO Launch algorithm GUI (jlopez)
	executeTest();
    }

    private void executeTest() {
	Sextante.initialize();
	final AlbertoConditionalCostAlgorithm alg = new AlbertoConditionalCostAlgorithm();

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
