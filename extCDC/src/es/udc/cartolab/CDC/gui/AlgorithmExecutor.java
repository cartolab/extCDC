package es.udc.cartolab.CDC.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;
import com.iver.cit.gvsig.project.documents.view.gui.BaseView;

import es.udc.cartolab.CDC.utils.CDCParameters;
import es.udc.sextante.gridAnalysis.CDC.CDCAlgorithm;
import es.unex.sextante.core.AnalysisExtent;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.dataObjects.IDataObject;
import es.unex.sextante.gvsig.core.gvOutputFactory;
import es.unex.sextante.gvsig.core.gvRasterLayer;
import es.unex.sextante.outputs.Output;

public class AlgorithmExecutor extends JPanel implements IWindow, ActionListener {

    private JProgressBar progressBar;
    private JLabel taskOutput;
    private JLabel timeOutput;
    private Task task;
    private CDCParameters parameters;
    private WindowInfo viewInfo = null;
    private JPanel panelButtons = null;
    private JButton okButton = null;
    private BaseView view;
    private OutputObjectsSet output;
    private Date initTime;

    private AlgorithmExecutor(CDCParameters parameters, BaseView view) {
        super();
        this.view = view;
        this.parameters = parameters;
        this.initTime = new Date();
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            try {
                CDCAlgorithm algorithm = new CDCAlgorithm();

                gvRasterLayer scs = new gvRasterLayer(), origin = new gvRasterLayer(), zonas_despl = new gvRasterLayer();
                scs.create(parameters.getScs());
                origin.create(parameters.getOrigen());
                zonas_despl.create(parameters.getZonaDespl());
                scs.open();
                origin.open();
                zonas_despl.open();

                output = algorithm.compute(parameters
                        .getMovementConstraintsTable(), parameters
                        .getMovementSurfaceGroupsTable(), parameters
                        .getSccSextante(), zonas_despl, scs, origin, 0,
                        parameters.getDestino().getAbsolutePath(),
                        new gvOutputFactory(), new AnalysisExtent(origin));

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /*
         * Executed in event dispatch thread
         */
        @Override
        public void done() {
            String durationMessage = timeOutput.getText();
            System.out.println("Milisegundos de procesamiento: " + (new Date().getTime() - initTime.getTime()));
            durationMessage = durationMessage.replace("---",  DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date()));

            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            Toolkit.getDefaultToolkit().beep();
            Font newLabelFont = new Font(taskOutput.getFont().getName(),
                    Font.BOLD, taskOutput.getFont().getSize() + 2);
            taskOutput.setFont(newLabelFont);
            taskOutput.setText(PluginServices.getText(this,
 "_CDC-executed"));
            timeOutput.setText(durationMessage);
            okButton.setEnabled(true);
        }
    }

    public void initialize() throws Exception {

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        // FILA 1
        c.weightx = 1.0;
        c.ipady = 5;
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = 1;

        super.setLayout(layout);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        layout.setConstraints(progressBar, c);

        super.add(progressBar);

        // FILA 2
        c.gridy++;
        c.gridwidth = 5;

		taskOutput = new JLabel(PluginServices.getText(this, "_CDC-executing"));
        taskOutput.setHorizontalAlignment(SwingConstants.CENTER);
        Font newLabelFont = new Font(taskOutput.getFont().getName(),
                Font.PLAIN, taskOutput.getFont().getSize() + 2);
        taskOutput.setFont(newLabelFont);
        layout.setConstraints(taskOutput, c);

        super.add(taskOutput);

        // FILA 3
        c.gridy++;
        c.gridwidth = 5;

		timeOutput = new JLabel("<html>"
				+ PluginServices.getText(this, "_CDC-init_time")
				+
                ": " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(initTime) +
 "<br>"
				+ PluginServices.getText(this, "_CDC-end_time")
				+ ": ---</html></html>");
        timeOutput.setHorizontalAlignment(SwingConstants.CENTER);
        newLabelFont = new Font(timeOutput.getFont().getName(),
                Font.PLAIN, timeOutput.getFont().getSize());
        timeOutput.setFont(newLabelFont);
        layout.setConstraints(timeOutput, c);

        super.add(timeOutput);

        c.gridy++;
        c.gridwidth = 2;
        c.ipady = 8;
        c.insets = new Insets(12, 0, 0, 0);
        c.fill = GridBagConstraints.CENTER;
        c.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = getJPanelButtons();
        layout.setConstraints(buttonPanel, c);
        super.add(buttonPanel);

        okButton.setEnabled(false);

    }

    private JPanel getJPanelButtons() {
        if (panelButtons == null) {
            panelButtons = new JPanel();
            panelButtons.add(getOkButton());
        }
        return panelButtons;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText(PluginServices.getText(this, "OK"));
            okButton.addActionListener(this);
        }
        return okButton;
    }

    public void start() {
        task = new Task();
        task.execute();
    }

    /**
     * Create the GUI and show it. As with all GUI code, this must run on the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(CDCParameters parameters) {

        IWindow window = PluginServices.getMDIManager().getActiveWindow();
        BaseView view = null;
        if (window instanceof BaseView) {
            view = (BaseView) window;
        }
        AlgorithmExecutor commandDialog = new AlgorithmExecutor(parameters,
                view);
        commandDialog.start();
        window = PluginServices.getMDIManager().addWindow(commandDialog);

    }

    @Override
    public WindowInfo getWindowInfo() {
        if (viewInfo == null) {
            viewInfo = new WindowInfo(WindowInfo.MODALDIALOG
                    | WindowInfo.PALETTE | WindowInfo.NOTCLOSABLE);
            viewInfo.setTitle(PluginServices.getText(this,
					"_CDC-executing-title"));
            viewInfo.setWidth(350);
            viewInfo.setHeight(170);
        }
        return viewInfo;
    }

    @Override
    public Object getWindowProfile() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            loadLayers();
            PluginServices.getMDIManager().closeWindow(this);
        }
    }// actionPerformed

    private void loadLayers() {
        try {
            for (int i = 0; i < output.getOutputObjectsCount(); i++) {
                final Output out = output.getOutput(i);
                final Object obj = out.getOutputObject();
                if (obj instanceof IDataObject) {
                    final IDataObject dataObject = (IDataObject) obj;
                    if (obj instanceof gvRasterLayer && view != null) {
                        dataObject.postProcess();
                        Object layer = ((gvRasterLayer) obj)
                                .getBaseDataObject();
                        if (layer instanceof FLyrRasterSE) {
                            view.getMapControl().getMapContext().getLayers()
                                    .addLayer((FLyrRasterSE) layer);
                        } else {
                            dataObject.free();
                            dataObject.open();
                            layer = ((gvRasterLayer) obj).getBaseDataObject();
                            if (layer instanceof FLyrRasterSE) {
                                view.getMapControl().getMapContext()
                                        .getLayers().addLayer(
                                                (FLyrRasterSE) layer);
                            }
                        }
                        // Cerrar las capas ser�a lo correcto, pero consume una cantidad decente de tiempo
                        dataObject.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}