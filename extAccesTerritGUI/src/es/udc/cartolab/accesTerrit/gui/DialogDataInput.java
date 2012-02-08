package es.udc.cartolab.accesTerrit.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileSystemView;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;

import es.udc.cartolab.accesTerrit.utils.AccesTerritParameters;
import es.udc.cartolab.accesTerrit.utils.CsvFilter;

public class DialogDataInput extends JPanel implements IWindow, ActionListener {

    private static final long serialVersionUID = 1L;
    private WindowInfo viewInfo = null;
    private JButton cancelButton = null;
    private JButton okButton = null;
    private JButton loadButton = null;
    private JButton sccButton = null;
    private JLabel sccLabel = null;
    private JPanel panelButtons = null;
    private JComboBox comboZDRaster = null;
    private JComboBox comboInputRaster = null;
    private JComboBox comboSCSRaster = null;
    private JTextField fieldOutputDirectory = null;
    private JButton dotsButtonOutput = null;
    private FLyrRasterSE[] rasters;
    private Collection<FLyrRasterSE> sccs = new HashSet<FLyrRasterSE>();
    private Vector<String> options;
    private AccesTerritParameters parameters;

    public WindowInfo getWindowInfo() {
        if (viewInfo == null) {
            viewInfo = new WindowInfo(WindowInfo.MODALDIALOG
                    | WindowInfo.PALETTE);
            viewInfo.setTitle(PluginServices.getText(this, "Input"));
            if ((sccLabel != null) && (sccButton != null)) {
                viewInfo.setWidth(sccLabel.getPreferredSize().width
                        + sccButton.getPreferredSize().width + 50);
            } else {
                viewInfo.setWidth(480);
            }
            viewInfo.setHeight(180);
        }
        return viewInfo;

    }

    public DialogDataInput(FLyrRasterSE[] rasters) {
        super();
        this.parameters = new AccesTerritParameters();
        this.rasters = rasters;
        options = new Vector<String>();
        for (FLyrRasterSE raster : rasters) {
            options.add(raster.getName());
        }

        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DialogDataInput(AccesTerritParameters parameters,
            FLyrRasterSE[] rasters) {
        super();
        this.parameters = parameters;
        this.rasters = rasters;
        this.sccs = parameters.getScc();
        options = new Vector<String>();
        for (FLyrRasterSE raster : rasters) {
            options.add(raster.getName());
        }

        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < options.size(); i++) {
            if ((parameters.getOrigen() != null)
                    && (options.get(i).equals(parameters.getOrigen().getName()))) {
                comboInputRaster.setSelectedIndex(i);
            }
            if ((parameters.getScs() != null)
                    && (options.get(i).equals(parameters.getScs().getName()))) {
                comboSCSRaster.setSelectedIndex(i);
            }
            if ((parameters.getZonaDespl() != null)
                    && (options.get(i).equals(parameters.getZonaDespl()
                            .getName()))) {
                comboZDRaster.setSelectedIndex(i);
            }
        }

        if (parameters.getDestino() != null) {
            fieldOutputDirectory.setText(parameters.getDestino()
                    .getAbsolutePath());
        }

    }

    private class SCCRasterSelector extends JPanel implements IWindow,
            ActionListener {

        private JLabel[] labelsRasters;
        private JCheckBox[] checkBoxesScc;
        private JButton cancelButton = null;
        private JButton okButton = null;
        private JPanel panelButtons = null;
        private WindowInfo viewInfo = null;

        public SCCRasterSelector() {
            super();

            labelsRasters = new JLabel[rasters.length];
            checkBoxesScc = new JCheckBox[rasters.length];
            for (int i = 0; i < rasters.length; i++) {
                labelsRasters[i] = new JLabel(rasters[i].getName());
                labelsRasters[i].setHorizontalAlignment(SwingConstants.CENTER);
                checkBoxesScc[i] = new JCheckBox();
                checkBoxesScc[i].setSelected(sccs.contains(rasters[i]));
            }

            try {
                initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private JButton getCancelButton() {
            if (cancelButton == null) {
                cancelButton = new JButton();
                cancelButton.setText(PluginServices.getText(this, "Cancel"));
                cancelButton.addActionListener(this);
            }
            return cancelButton;
        }

        private JButton getOkButton() {
            if (okButton == null) {
                okButton = new JButton();
                okButton.setText(PluginServices.getText(this, "OK"));
                okButton.addActionListener(this);
            }
            return okButton;
        }

        private JPanel getJPanelButtons() {
            if (panelButtons == null) {
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                panelButtons = new JPanel();
                panelButtons.setLayout(layout);
                c.anchor = GridBagConstraints.SOUTHWEST;
                c.insets = new Insets(12, 6, 0,
                        getWindowInfo().getWidth() - 180);
                layout.setConstraints(getCancelButton(), c);
                c.anchor = GridBagConstraints.SOUTHEAST;
                c.insets = new Insets(12, 0, 0, 6);
                layout.setConstraints(getOkButton(), c);
                panelButtons.add(getCancelButton());
                panelButtons.add(getOkButton());
            }
            return panelButtons;
        }

        @Override
        public WindowInfo getWindowInfo() {
            if (viewInfo == null) {
                viewInfo = new WindowInfo(WindowInfo.MODALDIALOG
                        | WindowInfo.PALETTE);
                viewInfo.setTitle(PluginServices.getText(this, "SCCs"));
                viewInfo.setWidth(320);
                viewInfo.setHeight(50 + (30 * rasters.length));
            }
            return viewInfo;
        }

        @Override
        public Object getWindowProfile() {
            // TODO Auto-generated method stub
            return null;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == cancelButton) {
                PluginServices.getMDIManager().closeWindow(this);
            }
            if (e.getSource() == okButton) {

                sccs.clear();
                for (int i = 0; i < checkBoxesScc.length; i++) {
                    if (checkBoxesScc[i].isSelected()) {
                        sccs.add(rasters[i]);
                    }
                }
                PluginServices.getMDIManager().closeWindow(this);

            }// if okButton
        }// actionPerformed

        public void initialize() throws Exception {

            double p = TableLayout.PREFERRED;
            double f = TableLayout.FILL;
            double vg = 10, hg = 10;
            double rowsDef[] = new double[4 + (rasters.length * 2)];
            rowsDef[0] = vg;
            rowsDef[1] = p;
            for (int i = 0; i < rasters.length; i++) {
                rowsDef[2 + (i * 2)] = vg;
                rowsDef[3 + (i * 2)] = p;
            }
            rowsDef[rowsDef.length - 2] = p;
            rowsDef[rowsDef.length - 1] = vg;
            double size[][] = { { hg, 220, 100, hg }, rowsDef };

            TableLayout layout = new TableLayout(size);

            super.setLayout(layout);

            JLabel rasterLabel = new JLabel("Raster");
            Font newLabelFont = new Font(rasterLabel.getFont().getName(),
                    Font.BOLD, rasterLabel.getFont().getSize() + 2);
            rasterLabel.setFont(newLabelFont);
            rasterLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JLabel sccLabel = new JLabel("SCC");
            sccLabel.setFont(newLabelFont);
            sccLabel.setHorizontalAlignment(SwingConstants.CENTER);

            super.add(rasterLabel, "1, 1, CENTER, CENTER");
            super.add(sccLabel, "2, 1, CENTER, CENTER");

            for (int i = 0; i < rasters.length; i++) {
                super.add(labelsRasters[i], "1, "
                        + Integer.toString((2 * i) + 3) + ", LEFT, CENTER");
                super.add(checkBoxesScc[i], "2, "
                        + Integer.toString((2 * i) + 3) + ", CENTER, CENTER");
            }

            JPanel buttonPanel = getJPanelButtons();
            buttonPanel.setPreferredSize(new Dimension(getWindowInfo()
                    .getWidth(), 50));

            super.add(buttonPanel, "0, " + Integer.toString(rowsDef.length - 2)
                    + ", 3, " + Integer.toString(rowsDef.length - 2)
                    + ", LEFT, CENTER");

            okButton.getName();

        }
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText(PluginServices.getText(this, "Cancel"));
            cancelButton.addActionListener(this);
        }
        return cancelButton;
    }

    private JButton getLoadButton() {
        if (loadButton == null) {
            loadButton = new JButton();
            loadButton.setText(PluginServices.getText(this, "LoadCSV"));
            loadButton.addActionListener(this);
        }
        return loadButton;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText(PluginServices.getText(this, "Next"));
            okButton.addActionListener(this);
        }
        return okButton;
    }

    private JPanel getJPanelButtons() {
        if (panelButtons == null) {
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            panelButtons = new JPanel();
            panelButtons.setLayout(layout);
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.insets = new Insets(12, 0, 0, getWindowInfo().getWidth()
                    - (getOkButton().getPreferredSize().width
                            + getLoadButton().getPreferredSize().width
                            + getCancelButton().getPreferredSize().width + 40));
            layout.setConstraints(getCancelButton(), c);
            c.anchor = GridBagConstraints.SOUTHEAST;
            c.insets = new Insets(12, 0, 0, 6);
            layout.setConstraints(getOkButton(), c);
            layout.setConstraints(getLoadButton(), c);
            panelButtons.add(getCancelButton());
            panelButtons.add(getLoadButton());
            panelButtons.add(getOkButton());
        }
        return panelButtons;
    }

    private void initialize() throws Exception {

        sccButton = new JButton();
        sccButton.setText(PluginServices.getText(this, "ChooseSCCs"));
        sccButton.addActionListener(this);

        double p = TableLayout.PREFERRED;
        double vg = 10, hg = 10;
        double size[][] = { { hg, p, p, p, hg }, { vg, p, p, p, p, p, p, vg } };
        TableLayout layout = new TableLayout(size);
        layout.setHGap(10);
        layout.setVGap(10);

        // FILA 1

        super.setLayout(layout);
        JLabel ZDLabel = new JLabel(PluginServices.getText(this,
                "ZonaDesplazamiento"));
        comboZDRaster = new JComboBox(options);
        comboZDRaster.addActionListener(this);
        super.add(ZDLabel, "1, 1");

        comboZDRaster.setPreferredSize(new Dimension(200, 20));
        super.add(comboZDRaster, "2, 1, 3, 1, LEFT, CENTER");

        // FILA 2

        super.setLayout(layout);
        JLabel InputLabel = new JLabel(PluginServices.getText(this, "Origen"));
        comboInputRaster = new JComboBox(options);
        comboInputRaster.addActionListener(this);
        super.add(InputLabel, "1, 2");

        comboInputRaster.setPreferredSize(new Dimension(200, 20));
        super.add(comboInputRaster, "2, 2, 3, 2, LEFT, CENTER");

        // FILA 3

        super.setLayout(layout);
        JLabel SCSLabel = new JLabel(PluginServices.getText(this, "SCS"));
        comboSCSRaster = new JComboBox(options);
        comboSCSRaster.addActionListener(this);
        super.add(SCSLabel, "1, 3");

        comboSCSRaster.setPreferredSize(new Dimension(200, 20));
        super.add(comboSCSRaster, "2, 3, 3, 3, LEFT, CENTER");

        // FILA 4

        super.setLayout(layout);
        sccLabel = new JLabel(PluginServices.getText(this, "SCC"));
        super.add(sccLabel, "1, 4");

        sccButton.setPreferredSize(new Dimension(200, 20));
        super.add(sccButton, "2, 4, 3, 4, LEFT, CENTER");

        // FILA 5

        super.setLayout(layout);
        JLabel directoryLabel = new JLabel(PluginServices.getText(this,
                "Output"));
        fieldOutputDirectory = new JTextField(FileSystemView
                .getFileSystemView().getDefaultDirectory().getAbsolutePath());
        super.add(directoryLabel, "1, 5");

        fieldOutputDirectory.setPreferredSize(new Dimension(170, 20));
        super.add(fieldOutputDirectory, "2, 5");

        /*
         * JLabel badLabel = new JLabel(" "); super.add(badLabel);
         */

        dotsButtonOutput = new JButton("...");
        dotsButtonOutput.addActionListener(this);
        dotsButtonOutput.setPreferredSize(new Dimension(20, 20));
        super.add(dotsButtonOutput, "3, 5, LEFT, CENTER");
        JPanel buttonPanel = getJPanelButtons();
        buttonPanel.setPreferredSize(new Dimension(getWindowInfo().getWidth(),
                50));

        // okButton.setEnabled(false);

        super.add(buttonPanel, "0, 6, 4, 6, LEFT, CENTER");

    }

    private Integer[] retrieveClasses(FLyrRasterSE zd) {

        int m_iNX = (int) zd.getDataSource().getWidth();
        int m_iNY = (int) zd.getDataSource().getHeight();

        // COUNT DIFFERENT VALUES
        final HashSet<Integer> surfacesID = new HashSet<Integer>();
        for (int y = 0; y < m_iNY; y++) {
            for (int x = 0; x < m_iNX; x++) {
                int value;
                try {
                    value = (Integer) zd.getDataSource().getData(x, y, 0);
                    if (value != (int) zd.getDataSource().getNoDataValue()
                            && !surfacesID.contains(value)) {
                        surfacesID.add(value);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return surfacesID.toArray(new Integer[0]);

    }

    private void loadCsv() {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new CsvFilter());
        chooser.setDialogTitle(PluginServices.getText(this, "ChooseFile"));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File csv = new File(chooser.getSelectedFile().getAbsolutePath());
            if (!csv.canRead()) {
                JOptionPane.showMessageDialog(this, PluginServices.getText(
                        this, "ErrorCantReadFileMessage"), PluginServices
                        .getText(this, "ErrorCantReadFileTitle"),
                        JOptionPane.ERROR_MESSAGE);
                loadCsv();
                return;
            }

            parameters.loadFromCSV(csv, rasters);
            DialogDataInput dialog = new DialogDataInput(parameters, rasters);
            PluginServices.getMDIManager().closeWindow(this);
            PluginServices.getMDIManager().addWindow(dialog);
            return;

        }

    }

    private boolean checkUniqueRasters() {
        HashSet<FLyrRasterSE> set = new HashSet<FLyrRasterSE>();
        boolean unique = true;
        unique &= set.add(parameters.getOrigen());
        unique &= set.add(parameters.getZonaDespl());
        unique &= set.add(parameters.getScs());
        for (FLyrRasterSE raster : parameters.getScc()) {
            unique &= set.add(raster);
        }
        return unique;
    }

    private boolean checkScsSccSameSize() {
        FLyrRasterSE scs = parameters.getScs();
        int x = (int) scs.getDataSource().getWidth();
        int y = (int) scs.getDataSource().getHeight();

        for (FLyrRasterSE scc : parameters.getScc()) {
            if ((((int) scc.getDataSource().getWidth()) != x)
                    || (((int) scc.getDataSource().getHeight()) != y)) {
                return false;
            }
        }

        return true;
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == dotsButtonOutput) {
            File currentDirectory = new File(fieldOutputDirectory.getText());
            JFileChooser chooser;
            if (currentDirectory.isDirectory()) {
                chooser = new JFileChooser(currentDirectory);
            } else {
                chooser = new JFileChooser();
            }
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                fieldOutputDirectory.setText(chooser.getSelectedFile()
                        .getAbsolutePath());
            }
            return;
        }

        if (e.getSource() == cancelButton) {
            PluginServices.getMDIManager().closeWindow(this);
            return;
        }

        if (e.getSource() == loadButton) {

            loadCsv();

        }// if okButton

        if (e.getSource() == okButton) {

            if (comboZDRaster.getSelectedIndex() != -1) {

                FLyrRasterSE zd = rasters[comboZDRaster.getSelectedIndex()];
                if ((this.parameters.getZonaDespl() != rasters[comboZDRaster
                        .getSelectedIndex()])
                        || (this.parameters.getClasses() == null)) {
                    Integer[] classes = retrieveClasses(zd);
                    this.parameters.initializeClasses(classes);
                }
                this.parameters.setOrigen(rasters[comboInputRaster
                        .getSelectedIndex()]);
                this.parameters.setZonaDespl(rasters[comboZDRaster
                        .getSelectedIndex()]);
                this.parameters.setScs(rasters[comboSCSRaster
                        .getSelectedIndex()]);
                this.parameters.setScc(sccs);
                this.parameters.setDestino(new File(fieldOutputDirectory
                        .getText()));

                if (!checkUniqueRasters()) {
                    JOptionPane.showMessageDialog(this, PluginServices.getText(
                            this, "ErrorUniqueRastersMessage"), PluginServices
                            .getText(this, "ErrorUniqueRastersTitle"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!checkScsSccSameSize()) {
                    JOptionPane.showMessageDialog(this, PluginServices.getText(
                            this, "ErrorSCCMessage"), PluginServices.getText(
                            this, "ErrorSCCTitle"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                DialogZDDefinition dialog = new DialogZDDefinition(parameters,
                        rasters);
                PluginServices.getMDIManager().closeWindow(this);
                PluginServices.getMDIManager().addWindow(dialog);
                return;
            }

        }// if okButton

        if (e.getSource() == sccButton) {

            SCCRasterSelector dialog = new SCCRasterSelector();
            PluginServices.getMDIManager().addWindow(dialog);
            return;

        }

    }// actionPerformed

    @Override
    public Object getWindowProfile() {
        // TODO Auto-generated method stub
        return null;
    }
}
