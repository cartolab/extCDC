package es.udc.cartolab.accesTerrit;

import org.apache.log4j.Logger;

import com.iver.andami.PluginServices;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.project.documents.view.gui.BaseView;

import es.udc.cartolab.accesTerrit.gui.DialogDataInput;
import es.unex.sextante.core.IInputFactory;
import es.unex.sextante.dataObjects.IRasterLayer;
import es.unex.sextante.gui.core.SextanteGUI;

public class AccesTerritGuiExtension extends Extension {

    private BaseView view = null;
    private IRasterLayer[] rasters;

    private static Logger logger = Logger.getLogger("AccesTerrit");

    public void execute(String actionCommand) {

	if (actionCommand.equals("gui")) {

	    IInputFactory input = SextanteGUI.getInputFactory();
	    input.createDataObjects();
	    rasters = SextanteGUI.getInputFactory().getRasterLayers();
	    DialogDataInput dialog = new DialogDataInput(rasters);

	    PluginServices.getMDIManager().addWindow(dialog);
	}

    }

    protected void registerIcons() {
	PluginServices.getIconTheme().registerDefault(
		"acces_territ-icon",
		this.getClass().getClassLoader().getResource(
			"images/algorithm.png"));
    }

    public void initialize() {
	// registerIcons();
	/*
	 * About about = (About) PluginServices.getExtension(About.class);
	 * FPanelAbout panelAbout = about.getAboutPanel(); java.net.URL aboutURL
	 * = this.getClass().getResource("/about.htm");
	 * panelAbout.addAboutUrl("PlanSan Priorización", aboutURL);
	 */
    }

    public boolean isEnabled() {
	return true;
    }

    public boolean isVisible() {
	return true;
    }
}
