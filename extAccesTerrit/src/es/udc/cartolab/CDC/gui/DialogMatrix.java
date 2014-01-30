package es.udc.cartolab.CDC.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.gvsig.fmap.raster.layers.FLyrRasterSE;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.andami.ui.mdiManager.WindowInfo;

import es.udc.cartolab.CDC.utils.CDCParameters;
import es.udc.cartolab.CDC.utils.CsvFilter;

public class DialogMatrix extends JPanel implements IWindow, ActionListener {

    private static final long serialVersionUID = 1L;
    private WindowInfo viewInfo = null;
    private JButton cancelButton = null;
    private JButton prevButton = null;
    private JButton okButton = null;
    private JButton saveButton = null;
    private JButton checkButton = null;
    private JButton uncheckButton = null;
    private JPanel panelButtons = null;
    private JLabel[] labelsClassesRow;
    private JLabel[] labelsClassesColumn;
    private JCheckBox[][] checkBoxesNode;
    private FLyrRasterSE[] rasters;
    private CDCParameters parameters;

    public WindowInfo getWindowInfo() {
        if (viewInfo == null) {
            viewInfo = new WindowInfo(WindowInfo.MODALDIALOG
                    | WindowInfo.PALETTE);
            viewInfo.setTitle(PluginServices.getText(this, "Matrix"));
            int width = 40 + (40 * parameters.getClasses().size());
            if (width < 380) {
                width = 380;
            }
            viewInfo.setWidth(width);
            viewInfo.setHeight(95 + (35 * parameters.getClasses().size()));
        }
        return viewInfo;

    }

    public DialogMatrix(CDCParameters parameters, FLyrRasterSE[] rasters) {
        super();
        this.rasters = rasters;
        this.parameters = parameters;
        labelsClassesRow = new JLabel[parameters.getClasses().size()];
        labelsClassesColumn = new JLabel[parameters.getClasses().size()];
        checkBoxesNode = new JCheckBox[parameters.getClasses().size()][parameters
                .getClasses().size()];
        for (int i = 0; i < parameters.getClasses().size(); i++) {
            labelsClassesRow[i] = new JLabel(Integer.toString(parameters
                    .getClasses().get(i).getClase()));
            Font newLabelFont = new Font(labelsClassesRow[i].getFont()
                    .getName(), Font.BOLD, labelsClassesRow[i].getFont()
                    .getSize() + 2);
            labelsClassesRow[i].setFont(newLabelFont);
            labelsClassesRow[i].setHorizontalAlignment(SwingConstants.CENTER);
            labelsClassesColumn[i] = new JLabel(Integer.toString(parameters
                    .getClasses().get(i).getClase()));
            labelsClassesColumn[i].setFont(newLabelFont);
            labelsClassesColumn[i]
                    .setHorizontalAlignment(SwingConstants.CENTER);
            for (int j = 0; j < parameters.getClasses().size(); j++) {
                checkBoxesNode[i][j] = new JCheckBox();
                if (parameters.getClasses().get(i).isNodo()
                        || parameters.getClasses().get(j).isNodo()) {
                    checkBoxesNode[i][j].setSelected(true);
                }
            }
            checkBoxesNode[i][i].setSelected(true);
        }
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (parameters.getMatriz() != null) {
            boolean fill = true;

            if (parameters.getMatriz().size() != parameters.getClasses().size()) {
                fill = false;
            }

            for (Vector<Boolean> vector : parameters.getMatriz()) {
                if (vector.size() != parameters.getClasses().size()) {
                    fill = false;
                }
            }

            if (fill) {
                for (int i = 0; i < parameters.getMatriz().size(); i++) {
                    for (int j = 0; j < parameters.getMatriz().size(); j++) {
                        checkBoxesNode[i][j].setSelected(parameters.getMatriz()
                                .get(i).get(j).booleanValue());
                    }
                }
            }
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

    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton();
            saveButton.setText(PluginServices.getText(this, "SaveCSV"));
            saveButton.addActionListener(this);
        }
        return saveButton;
    }

    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText(PluginServices.getText(this, "OK"));
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

    private JButton getCheckButton() {
        if (checkButton == null) {
            checkButton = new JButton();
            checkButton.setText(PluginServices.getText(this, "CheckAll"));
            checkButton.addActionListener(this);
        }
        return checkButton;
    }

    private JButton getUncheckButton() {
        if (uncheckButton == null) {
            uncheckButton = new JButton();
            uncheckButton.setText(PluginServices.getText(this, "UncheckAll"));
            uncheckButton.addActionListener(this);
        }
        return uncheckButton;
    }

    private JPanel getJPanelButtons() {
        if (panelButtons == null) {
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            panelButtons = new JPanel();
            panelButtons.setLayout(layout);
            c.gridy = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(12, 6, 0, 0);
            layout.setConstraints(getUncheckButton(), c);
            c.anchor = GridBagConstraints.NORTHEAST;
            c.insets = new Insets(12, 0, 0, 6);
            c.gridwidth = 3;
            layout.setConstraints(getCheckButton(), c);
            panelButtons.add(getUncheckButton());
            panelButtons.add(getCheckButton());
            c.gridy += 2;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.insets = new Insets(12, 6, 0, getWindowInfo().getWidth() - 350);
            layout.setConstraints(getCancelButton(), c);
            c.anchor = GridBagConstraints.SOUTHEAST;
            c.insets = new Insets(12, 0, 0, 6);
            layout.setConstraints(getPrevButton(), c);
            layout.setConstraints(getSaveButton(), c);
            layout.setConstraints(getOkButton(), c);
            panelButtons.add(getCancelButton());
            panelButtons.add(getPrevButton());
            panelButtons.add(getSaveButton());
            panelButtons.add(getOkButton());
        }
        return panelButtons;
    }

    private void initialize() throws Exception {

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        c.weightx = 1.0;
        c.ipady = 5;
        c.gridy = 1;
        c.insets = new Insets(6, 6, 6, 0);
        super.setLayout(layout);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        JLabel voidLabel = new JLabel("O \\ D");
        Font newLabelFont = new Font(voidLabel.getFont().getName(), Font.BOLD,
                voidLabel.getFont().getSize() + 2);
        voidLabel.setFont(newLabelFont);
        voidLabel.setHorizontalAlignment(SwingConstants.CENTER);
        layout.setConstraints(voidLabel, c);
        super.add(voidLabel);
        for (int i = 0; i < parameters.getClasses().size(); i++) {
            layout.setConstraints(labelsClassesRow[i], c);
            super.add(labelsClassesRow[i]);
        }

        c.insets = new Insets(6, 6, 6, 0);
        for (int i = 0; i < parameters.getClasses().size(); i++) {
            c.gridy++;
            layout.setConstraints(labelsClassesColumn[i], c);
            super.add(labelsClassesColumn[i]);
            for (int j = 0; j < parameters.getClasses().size(); j++) {
                layout.setConstraints(checkBoxesNode[i][j], c);
                super.add(checkBoxesNode[i][j]);
            }
        }

        c.gridy += 2;
        c.ipady = 8;
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 0, 0, 0);
        c.gridwidth = parameters.getClasses().size() + 1;
        JPanel buttonPanel = getJPanelButtons();
        layout.setConstraints(buttonPanel, c);

        super.add(buttonPanel);

    }

    private void saveCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new CsvFilter());
        chooser.setApproveButtonText(PluginServices.getText(this, "OK"));
        chooser.setDialogTitle(PluginServices.getText(this, "ChooseFile"));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".csv")) {
                path += ".csv";
            }
            File csv = new File(path);
            if (csv.exists()) {
                switch (JOptionPane.showConfirmDialog(this, PluginServices
                        .getText(this, "ReplaceFileMessage"), PluginServices
                        .getText(this, "ReplaceFileTitle"),
                        JOptionPane.YES_NO_CANCEL_OPTION)) {
                case JOptionPane.CANCEL_OPTION:
                    return;
                case JOptionPane.NO_OPTION:
                    saveCsv();
                    return;
                case JOptionPane.YES_OPTION:
                    break;
                }
            }
            try {
                if ((csv.canWrite()) || (!csv.exists() && csv.createNewFile())) {
                    parameters.writeToCSV(csv);
                } else {
                    JOptionPane.showMessageDialog(this, PluginServices.getText(
                            this, "ErrorCsvSaveMessage"), PluginServices
                            .getText(this, "ErrorCsvSaveTitle"),
                            JOptionPane.ERROR_MESSAGE);
                    saveCsv();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, PluginServices.getText(
                        this, "ErrorCsvSaveMessage"), PluginServices.getText(
                        this, "ErrorCsvSaveTitle"), JOptionPane.ERROR_MESSAGE);
                saveCsv();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        if (e.getSource() == cancelButton) {
            PluginServices.getMDIManager().closeWindow(this);
            return;
        }
        if (e.getSource() == saveButton) {

            Vector<Vector<Boolean>> matrix = new Vector<Vector<Boolean>>();

            for (int i = 0; i < parameters.getClasses().size(); i++) {
                Vector<Boolean> row = new Vector<Boolean>();
                for (int j = 0; j < parameters.getClasses().size(); j++) {
                    row.add(new Boolean(checkBoxesNode[i][j].isSelected()));
                }
                matrix.add(row);
            }

            parameters.setMatriz(matrix);

            saveCsv();

            return;
        }
        if (e.getSource() == okButton) {

            Vector<Vector<Boolean>> matrix = new Vector<Vector<Boolean>>();

            for (int i = 0; i < parameters.getClasses().size(); i++) {
                Vector<Boolean> row = new Vector<Boolean>();
                for (int j = 0; j < parameters.getClasses().size(); j++) {
                    row.add(new Boolean(checkBoxesNode[i][j].isSelected()));
                }
                matrix.add(row);
            }

            parameters.setMatriz(matrix);

            PluginServices.getMDIManager().closeWindow(this);
            (new Thread(new Runnable() {
                public void run() {
                    AlgorithmExecutor.createAndShowGUI(parameters);
                }
            })).start();

            return;

        }// if okButton

        if (e.getSource() == prevButton) {

            Vector<Vector<Boolean>> matrix = new Vector<Vector<Boolean>>();

            for (int i = 0; i < parameters.getClasses().size(); i++) {
                Vector<Boolean> row = new Vector<Boolean>();
                for (int j = 0; j < parameters.getClasses().size(); j++) {
                    row.add(new Boolean(checkBoxesNode[i][j].isSelected()));
                }
                matrix.add(row);
            }

            parameters.setMatriz(matrix);

            DialogZDDefinition dialog = new DialogZDDefinition(parameters,
                    rasters);
            PluginServices.getMDIManager().closeWindow(this);
            PluginServices.getMDIManager().addWindow(dialog);
            return;

        }// if prevButton

        if (e.getSource() == checkButton) {

            for (int i = 0; i < checkBoxesNode.length; i++) {
                for (int j = 0; j < checkBoxesNode[i].length; j++) {
                    checkBoxesNode[i][j].setSelected(true);
                }
            }
            return;

        }// if checkButton

        if (e.getSource() == uncheckButton) {

            for (int i = 0; i < checkBoxesNode.length; i++) {
                for (int j = 0; j < checkBoxesNode[i].length; j++) {
                    checkBoxesNode[i][j].setSelected(false);
                }
            }
            return;

        }// if uncheckButton
    }// actionPerformed

    public void executeAlgorithm() {

    }

    @Override
    public Object getWindowProfile() {
        return null;
    }
}
