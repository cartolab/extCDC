package es.udc.cartolab.accesTerrit.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;

import es.udc.cartolab.accesTerrit.utils.AccesTerritParameters;
import es.udc.cartolab.accesTerrit.utils.AreaClass;

public class DialogZDDefinition extends JPanel implements IWindow,
        ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private WindowInfo viewInfo = null;
    private JButton cancelButton = null;
    private JButton okButton = null;
    private JButton prevButton = null;
    private JPanel panelButtons = null;
    private JComboBox[] combosRaster;
    private JLabel[] labelsClasses;
    private JCheckBox[] checkBoxesNode;
    private JTextField[] textFieldsNames;
    private FLyrRasterSE[] rasters;
    private Vector<String> options;
    private AccesTerritParameters parameters;

    public WindowInfo getWindowInfo() {
        // TODO Auto-generated method stub
        if (viewInfo == null) {
            viewInfo = new WindowInfo(WindowInfo.MODALDIALOG
                    | WindowInfo.PALETTE);
            viewInfo.setTitle(PluginServices.getText(this, "ZD_Definition"));
            viewInfo.setWidth(labelsClasses[0].getPreferredSize().width
                    + textFieldsNames[0].getPreferredSize().width
                    + combosRaster[0].getPreferredSize().width
                    + checkBoxesNode[0].getPreferredSize().width + 240);
            viewInfo.setHeight(50 + (30 * this.parameters.getClasses().size()));
        }
        return viewInfo;

    }

    public DialogZDDefinition(AccesTerritParameters parameters,
            FLyrRasterSE[] rasters) {
        super();
        this.parameters = parameters;
        this.rasters = rasters;
        options = new Vector<String>();
        options.add("");
        for (FLyrRasterSE raster : parameters.getScc()) {
            options.add(raster.getName());
        }
        Vector<AreaClass> classes = this.parameters.getClasses();
        combosRaster = new JComboBox[classes.size()];
        labelsClasses = new JLabel[classes.size()];
        checkBoxesNode = new JCheckBox[classes.size()];
        textFieldsNames = new JTextField[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            labelsClasses[i] = new JLabel(Integer.toString(classes.get(i)
                    .getClase()));
            labelsClasses[i].setHorizontalAlignment(SwingConstants.CENTER);
            textFieldsNames[i] = new JTextField();
            if (classes.get(i).getNombre() != null) {
                textFieldsNames[i].setText(classes.get(i).getNombre());
            }
            textFieldsNames[i].setPreferredSize(new Dimension(150, 20));
            combosRaster[i] = new JComboBox(options);
            combosRaster[i].setPreferredSize(new Dimension(200, 20));
            if (classes.get(i).getEdc() != null) {
                for (int j = 1; j < options.size(); j++) {
                    if (options.get(j)
                            .equals(classes.get(i).getEdc().getName())) {
                        combosRaster[i].setSelectedIndex(j);
                    }
                }
                textFieldsNames[i].setText(classes.get(i).getNombre());
            }
            combosRaster[i].addActionListener(this);
            checkBoxesNode[i] = new JCheckBox();
            checkBoxesNode[i].setSelected(classes.get(i).isNodo());

            if (combosRaster[i].getSelectedIndex() == 0) {
                checkBoxesNode[i].setEnabled(false);
            }
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
            okButton.setText(PluginServices.getText(this, "Next"));
            okButton.addActionListener(this);
        }
        return okButton;
    }

    private JButton getPrevButton() {
        if (prevButton == null) {
            prevButton = new JButton();
            prevButton.setText(PluginServices.getText(this, "Previous"));
            prevButton.addActionListener(this);
        }
        return prevButton;
    }

    private JPanel getJPanelButtons() {
        if (panelButtons == null) {
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            panelButtons = new JPanel();
            panelButtons.setLayout(layout);
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.insets = new Insets(12, 6, 0, 0);
            layout.setConstraints(getCancelButton(), c);
            c.anchor = GridBagConstraints.SOUTHEAST;
            c.insets = new Insets(12, getWindowInfo().getWidth()
                    - (getCancelButton().getPreferredSize().width
                            + getPrevButton().getPreferredSize().width
                            + getOkButton().getPreferredSize().width + 50), 0, 6);
            layout.setConstraints(getPrevButton(), c);
            c.insets = new Insets(12, 6, 0, 0);
            layout.setConstraints(getOkButton(), c);
            panelButtons.add(getCancelButton());
            panelButtons.add(getPrevButton());
            panelButtons.add(getOkButton());
        }
        return panelButtons;
    }

    private void initialize() throws Exception {

        double p = TableLayout.PREFERRED;
        double f = TableLayout.FILL;
        double vg = 10, hg = 10;
        double rowsDef[] = new double[4 + (this.parameters.getClasses().size() * 2)];
        rowsDef[0] = vg;
        rowsDef[1] = p;
        for (int i = 0; i < this.parameters.getClasses().size(); i++) {
            rowsDef[2 + (i * 2)] = vg;
            rowsDef[3 + (i * 2)] = p;
        }
        rowsDef[rowsDef.length - 2] = p;
        rowsDef[rowsDef.length - 1] = vg;
        double size[][] = { { hg, 100, p, hg, p, 100, hg }, rowsDef };

        TableLayout layout = new TableLayout(size);

        super.setLayout(layout);

        JLabel classLabel = new JLabel("Clase");
        Font newLabelFont = new Font(classLabel.getFont().getName(), Font.BOLD,
                classLabel.getFont().getSize() + 2);
        classLabel.setFont(newLabelFont);
        classLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel nameLabel = new JLabel("Nombre");
        nameLabel.setFont(newLabelFont);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel rasterLabel = new JLabel("EDC");
        rasterLabel.setFont(newLabelFont);
        rasterLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel nodeLabel = new JLabel("Nodo");
        nodeLabel.setFont(newLabelFont);
        nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        super.add(classLabel, "1, 1, CENTER, CENTER");
        super.add(nameLabel, "2, 1, CENTER, CENTER");
        super.add(rasterLabel, "4, 1, CENTER, CENTER");
        super.add(nodeLabel, "5, 1, CENTER, CENTER");

        for (int i = 0; i < this.parameters.getClasses().size(); i++) {
            super.add(labelsClasses[i], "1, " + Integer.toString((2 * i) + 3)
                    + ", CENTER, CENTER");
            super.add(textFieldsNames[i], "2, " + Integer.toString((2 * i) + 3)
                    + ", CENTER, CENTER");
            super.add(combosRaster[i], "4, " + Integer.toString((2 * i) + 3)
                    + ", CENTER, CENTER");
            super.add(checkBoxesNode[i], "5, " + Integer.toString((2 * i) + 3)
                    + ", CENTER, CENTER");
        }

        JPanel buttonPanel = getJPanelButtons();
        buttonPanel.setPreferredSize(new Dimension(getWindowInfo().getWidth() - 25,
                50));

        // okButton.setEnabled(false);

        super.add(buttonPanel, "0, " + Integer.toString(rowsDef.length - 2)
                + ", 6, " + Integer.toString(rowsDef.length - 2)
                + ", LEFT, CENTER");

    }

    private void storeParameters() {

        for (int i = 0; i < this.parameters.getClasses().size(); i++) {
            this.parameters.getClasses().get(i).setNombre(
                    textFieldsNames[i].getText());
            if (combosRaster[i].getSelectedIndex() > 0) {
                for (FLyrRasterSE raster : parameters.getScc()) {
                    if (raster.getName().equals(
                            options.get(combosRaster[i].getSelectedIndex()))) {
                        this.parameters.getClasses().get(i).setEdc(raster);
                    }
                }
                this.parameters.getClasses().get(i).setNodo(
                        checkBoxesNode[i].isSelected());
            } else {
                this.parameters.getClasses().get(i).setEdc(null);
                this.parameters.getClasses().get(i).setNodo(false);
            }
        }

    }

    private boolean checkEdcNodes() {

        boolean[] noNodes = new boolean[options.size() - 1];

        for (int i = 0; i < noNodes.length; i++) {
            noNodes[i] = false;
        }

        for (int i = 0; i < this.parameters.getClasses().size(); i++) {
            if (combosRaster[i].getSelectedIndex() > 0) {
                noNodes[combosRaster[i].getSelectedIndex() - 1] |= (!checkBoxesNode[i]
                        .isSelected());
            }
        }

        for (int i = 0; i < noNodes.length; i++) {
            if (!noNodes[i]) {
                return false;
            }
        }

        return true;

    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        if (e.getSource() == cancelButton) {
            PluginServices.getMDIManager().closeWindow(this);
            return;
        }
        if (e.getSource() == okButton) {

            if (!checkEdcNodes()) {
                JOptionPane.showMessageDialog(this, PluginServices.getText(
                        this, "ErrorEdcMessage"), PluginServices.getText(this,
                        "ErrorEdcTitle"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            storeParameters();
            DialogMatrix dialog = new DialogMatrix(parameters, rasters);
            PluginServices.getMDIManager().closeWindow(this);
            PluginServices.getMDIManager().addWindow(dialog);

            return;
        }// if okButton

        if (e.getSource() == prevButton) {

            storeParameters();
            DialogDataInput dialog = new DialogDataInput(parameters, rasters);
            PluginServices.getMDIManager().closeWindow(this);
            PluginServices.getMDIManager().addWindow(dialog);
            return;

        }// if prevButton

        if (e.getSource() instanceof JComboBox) {
            for (int i = 0; i < combosRaster.length; i++) {
                if (e.getSource() == combosRaster[i]) {
                    if (combosRaster[i].getSelectedIndex() == 0) {
                        checkBoxesNode[i].setEnabled(false);
                    } else {
                        checkBoxesNode[i].setEnabled(true);
                    }
                    return;
                }
            }
        }

    }// actionPerformed

    @Override
    public Object getWindowProfile() {
        // TODO Auto-generated method stub
        return null;
    }
}
