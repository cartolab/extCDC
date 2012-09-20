package es.udc.cartolab.accesTerrit;

import java.util.ArrayList;
import java.util.List;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.iver.andami.PluginServices;
import com.iver.andami.plugins.Extension;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.cit.gvsig.fmap.layers.FLayer;
import com.iver.cit.gvsig.project.documents.view.gui.BaseView;

import es.udc.cartolab.accesTerrit.gui.DialogDataInput;

public class AccesTerritGuiExtension extends Extension {

    List<FLyrRasterSE> rasters;

    public void execute(String actionCommand) {

        if (actionCommand.equals("gui")) {
            DialogDataInput dialog = new DialogDataInput(rasters
                    .toArray(new FLyrRasterSE[0]));
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
        registerIcons();
    }

    public boolean isEnabled() {
        IWindow window = PluginServices.getMDIManager().getActiveWindow();
        if (window instanceof BaseView) {
            BaseView view = (BaseView) window;
            FLayer[] layers = view.getMapControl().getMapContext().getLayers().getVisibles();
            rasters = new ArrayList<FLyrRasterSE>();
            for (FLayer layer:layers) {
                if (layer instanceof FLyrRasterSE) {
                    rasters.add((FLyrRasterSE) layer);
                }
            }

            if (rasters.size() >= 3) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisible() {
        IWindow window = PluginServices.getMDIManager().getActiveWindow();
        if (window instanceof BaseView) {
            return true;
        }
        return false;
    }
}
