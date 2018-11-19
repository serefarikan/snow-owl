package org.protege.editor.owl.ui.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLAnnotationPropertyHierarchyProvider;
import org.protege.editor.owl.ui.selector.OWLAnnotationPropertySelectorPanel;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 10-Feb-2007<br><br>
 */
public class OWLAnnotationEditor extends AbstractOWLObjectEditor<OWLAnnotation> implements VerifiedInputEditor {


    private OWLEditorKit owlEditorKit;

    private JTabbedPane tabbedPane;

    private JPanel mainPanel;

    private OWLAnnotationPropertySelectorPanel annotationPropertySelector;

    private List<OWLObjectEditor<? extends OWLAnnotationValue>> editors;

    private OWLAnnotationProperty lastSelectedProperty;

    private List<InputVerificationStatusChangedListener> verificationListeners = new ArrayList<InputVerificationStatusChangedListener>();

    private boolean status = false;

    private ChangeListener changeListener = new ChangeListener(){
        public void stateChanged(ChangeEvent event) {
            verify();
        }
    };
    
    private InputVerificationStatusChangedListener mergedVerificationListener = new InputVerificationStatusChangedListener() {
		
		public void verifiedStatusChanged(final boolean newState) {
			for (InputVerificationStatusChangedListener listener : verificationListeners) {
				listener.verifiedStatusChanged(newState);
			}
		}
	};


    public OWLAnnotationEditor(OWLEditorKit owlEditorKit) {
        this.owlEditorKit = owlEditorKit;
        tabbedPane = new JTabbedPane();
        mainPanel = new VerifiedInputJPanel();
        mainPanel.setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.add(splitPane);

        final OWLModelManager mngr = owlEditorKit.getOWLModelManager();
        final OWLAnnotationPropertyHierarchyProvider hp =
                mngr.getOWLHierarchyManager().getOWLAnnotationPropertyHierarchyProvider();
        annotationPropertySelector = new OWLAnnotationPropertySelectorPanel(owlEditorKit, true, hp);
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.add(annotationPropertySelector);
        listHolder.setPreferredSize(new Dimension(200, 300));

        splitPane.setLeftComponent(listHolder);
        splitPane.setRightComponent(tabbedPane);
        splitPane.setBorder(null);
        loadEditors();
        lastSelectedProperty = mngr.getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());

        annotationPropertySelector.addSelectionListener(new ChangeListener(){
            public void stateChanged(ChangeEvent event) {
                verify();
            }
        });

        tabbedPane.addChangeListener(changeListener);
    }


    private void loadEditors() {
        final IRIFromEntityEditor iriEditor = new IRIFromEntityEditor(owlEditorKit);
        iriEditor.addSelectionListener(changeListener);

        final OWLConstantEditor constantEditor = new OWLConstantEditor(owlEditorKit);
        // @@TODO add change listener

        final OWLAnonymousIndividualAnnotationValueEditor anonIndividualEditor = new OWLAnonymousIndividualAnnotationValueEditor(owlEditorKit);
        // @@TODO add change listener
        
        final IRITextEditor textEditor = new IRITextEditor(owlEditorKit);
        textEditor.addStatusChangedListener(mergedVerificationListener);

        editors = new ArrayList<OWLObjectEditor<? extends OWLAnnotationValue>>();
        editors.add(constantEditor);
        editors.add(iriEditor);
        editors.add(textEditor);
        editors.add(anonIndividualEditor);
        for (OWLObjectEditor<? extends OWLAnnotationValue> editor : editors) {
            tabbedPane.add(editor.getEditorTypeName(), editor.getEditorComponent());
        }
    }


    private OWLObjectEditor<? extends OWLAnnotationValue> getSelectedEditor() {
        return editors.get(tabbedPane.getSelectedIndex());
    }


    public boolean setEditedObject(OWLAnnotation annotation) {
        int tabIndex = -1;
        if (annotation != null) {
            annotationPropertySelector.setSelection(annotation.getProperty());
            for (int i = 0; i < editors.size(); i++) {
                OWLObjectEditor editor = editors.get(i);
                // because we don't know the type of the editor we need to test
                if (editor.canEdit(annotation.getValue())) {
                    editor.setEditedObject(annotation.getValue());
                    if (tabIndex == -1) {
                        tabIndex = i;
                    }
                }
                else {
                    editor.setEditedObject(null);
                }
            }
        }
        else {
            annotationPropertySelector.setSelection(lastSelectedProperty);
            for (OWLObjectEditor<? extends OWLAnnotationValue> editor : editors) {
                editor.setEditedObject(null);
            }
        }
        tabbedPane.setSelectedIndex(tabIndex == -1 ? 0 : tabIndex);
        return true;
    }


    public OWLAnnotation getAnnotation() {
        OWLAnnotationProperty property = annotationPropertySelector.getSelectedObject();
        if (property != null){
            lastSelectedProperty = property;

            OWLDataFactory dataFactory = owlEditorKit.getModelManager().getOWLDataFactory();

            OWLAnnotationValue obj = getSelectedEditor().getEditedObject();

            if (obj != null) {
            	return dataFactory.getOWLAnnotation(property, obj);
            }
        }
        return null;
    }


    public String getEditorTypeName() {
        return "OWL Annotation";
    }


    public boolean canEdit(Object object) {
        return object instanceof OWLAnnotation;
    }


    public JComponent getEditorComponent() {
        return mainPanel;
    }


    public JComponent getInlineEditorComponent() {
        return getEditorComponent();
    }


    /**
     * Gets the object that has been edited.
     * @return The edited object
     */
    public OWLAnnotation getEditedObject() {
        return getAnnotation();
    }


    public void dispose() {
        annotationPropertySelector.dispose();
        for (OWLObjectEditor<? extends OWLAnnotationValue> editor : editors) {
            editor.dispose();
        }
    }


    private void verify() {
        if (status != isValid()){
            status = isValid();
            for (InputVerificationStatusChangedListener l : verificationListeners){
                l.verifiedStatusChanged(status);
            }
        }
    }


    private boolean isValid() {
        return annotationPropertySelector.getSelectedObject() != null && getSelectedEditor().getEditedObject() != null;
    }


    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        verificationListeners.add(listener);
        listener.verifiedStatusChanged(isValid());
    }


    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        verificationListeners.remove(listener);
    }
    
    private class VerifiedInputJPanel extends JPanel implements VerifiedInputEditor {
		private static final long serialVersionUID = -6537871629287844213L;

		public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
			OWLAnnotationEditor.this.addStatusChangedListener(listener);
		}

		public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
			OWLAnnotationEditor.this.removeStatusChangedListener(listener);
		}
    	
    }
}