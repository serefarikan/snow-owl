package org.protege.editor.owl.ui.preferences;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.protege.editor.core.ProtegeApplication;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.owl.model.find.OWLEntityFinderPreferences;
import org.protege.editor.owl.ui.clsdescriptioneditor.ExpressionEditorPreferences;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 03-Sep-2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public class GeneralPreferencesPanel extends OWLPreferencesPanel {

    //@@TODO centralise this when tidying up prefs
    private static final String DIALOGS_ALWAYS_CENTRED = "DIALOGS_ALWAYS_CENTRED";

    private JRadioButton simpleSearchButton;

    private JRadioButton regularExpressionSearchButton;

    private JSpinner findDelaySpinner;

    private JSpinner checkDelaySpinner;

    private static final String SECOND_TOOL_TIP = "1000 = 1 second";

    private JCheckBox alwaysCentreDialogsCheckbox;


    public void applyChanges() {
        ExpressionEditorPreferences.getInstance().setCheckDelay((Integer) checkDelaySpinner.getModel().getValue());

        OWLEntityFinderPreferences prefs = OWLEntityFinderPreferences.getInstance();
        prefs.setSearchDelay(((Double) findDelaySpinner.getModel().getValue()).intValue());
        prefs.setUseRegularExpressions(regularExpressionSearchButton.isSelected());

        Preferences appPrefs = PreferencesManager.getInstance().getApplicationPreferences(ProtegeApplication.ID);
        appPrefs.putBoolean(DIALOGS_ALWAYS_CENTRED, alwaysCentreDialogsCheckbox.isSelected());
    }


    public void initialise() throws Exception {
        setLayout(new BorderLayout());

        // editor box

        JPanel editorDelayPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final int checkDelay = ExpressionEditorPreferences.getInstance().getCheckDelay();
        checkDelaySpinner = new JSpinner(new SpinnerNumberModel(checkDelay, 0, 10000, 50));
        checkDelaySpinner.setToolTipText(SECOND_TOOL_TIP);
        editorDelayPanel.add(new JLabel("Editor delay (ms)"));
        editorDelayPanel.add(checkDelaySpinner);


        Box editorPanel = new Box(BoxLayout.PAGE_AXIS);
        editorPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Editor"),
                                                                 BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        editorPanel.add(editorDelayPanel);


        // search box

        OWLEntityFinderPreferences prefs = OWLEntityFinderPreferences.getInstance();
        findDelaySpinner = new JSpinner(new SpinnerNumberModel(prefs.getSearchDelay(), 0, 10000, 50));
        findDelaySpinner.setToolTipText(SECOND_TOOL_TIP);

        JPanel findDelayPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        findDelayPanel.add(new JLabel("Search delay (ms)"));
        findDelayPanel.add(findDelaySpinner);

        simpleSearchButton = new JRadioButton("Simple search (using simple wildcards *)",
                                              !prefs.isUseRegularExpressions());
        regularExpressionSearchButton = new JRadioButton("Full regular expression search",
                                                         prefs.isUseRegularExpressions());

        ButtonGroup bg = new ButtonGroup();
        bg.add(simpleSearchButton);
        bg.add(regularExpressionSearchButton);

        findDelayPanel.setAlignmentX(LEFT_ALIGNMENT);
        simpleSearchButton.setAlignmentX(LEFT_ALIGNMENT);
        regularExpressionSearchButton.setAlignmentX(LEFT_ALIGNMENT);

        Box searchPanel = new Box(BoxLayout.PAGE_AXIS);

        searchPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Search"),
                                                                 BorderFactory.createEmptyBorder(7, 7, 7, 7)));
        searchPanel.add(findDelayPanel);
        searchPanel.add(simpleSearchButton);
        searchPanel.add(regularExpressionSearchButton);

        Preferences appPrefs = PreferencesManager.getInstance().getApplicationPreferences(ProtegeApplication.ID);
        alwaysCentreDialogsCheckbox = new JCheckBox("Centre dialogs on workspace");
        alwaysCentreDialogsCheckbox.setSelected(appPrefs.getBoolean(DIALOGS_ALWAYS_CENTRED, false));

        editorPanel.setAlignmentX(LEFT_ALIGNMENT);
        searchPanel.setAlignmentX(LEFT_ALIGNMENT);
        alwaysCentreDialogsCheckbox.setAlignmentX(LEFT_ALIGNMENT);

        Box holder = new Box(BoxLayout.PAGE_AXIS);
        holder.add(editorPanel);
        holder.add(Box.createVerticalStrut(7));
        holder.add(searchPanel);
        holder.add(Box.createVerticalStrut(7));
        holder.add(alwaysCentreDialogsCheckbox);

        add(holder, BorderLayout.NORTH);
    }


    public void dispose() {
    }
}