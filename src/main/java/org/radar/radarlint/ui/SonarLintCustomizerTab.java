package org.radar.radarlint.ui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;
import org.radar.radarlint.EditorAnnotator;
import org.radar.radarlint.FileOpenedNotifier;
import org.radar.radarlint.SonarLintScanner;
import org.radar.radarlint.settings.ExcludedFilePatterns;
import org.radar.radarlint.settings.SettingsAccessor;
import org.radar.radarlint.settings.SonarLintActivePreference;
import org.radar.radarlint.settings.SonarLintProjectKeyPreference;

/**
 *
 * @author Víctor
 */
public class SonarLintCustomizerTab implements ProjectCustomizer.CompositeCategoryProvider {
    private final String name;

    public SonarLintCustomizerTab(String name) {
        this.name = name;
    }
    
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lookup) {
        return ProjectCustomizer.Category.create(name, name, null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, Lookup lookup) {
        SonarLintPropertiesComponent component = new SonarLintPropertiesComponent();
        Project currentProject = lookup.lookup(Project.class);
        Preferences preferences = ProjectUtils.getPreferences(currentProject, SonarLintPropertiesComponent.class, false);
        
        SettingsAccessor<Boolean> sonarLintActivePreference=new SonarLintActivePreference(preferences);
        SettingsAccessor<String> excludedFilePatternsPreference = new ExcludedFilePatterns(preferences);
        SettingsAccessor<String> sonarLintProjectKeyPreference = new SonarLintProjectKeyPreference(preferences);
       
        category.setOkButtonListener((ActionEvent e) -> {
            if(category.isValid()) {
                //save current properties
                sonarLintActivePreference.setValue(component.isSonarLintActive());
                excludedFilePatternsPreference.setValue(component.getExcludedFilePatterns());
                sonarLintProjectKeyPreference.setValue(component.getProjectKey());
               
                EditorAnnotator editorAnnotator = EditorAnnotator.getInstance();
                if(!component.isSonarLintActive()) {
                    editorAnnotator.cleanEditorAnnotations(currentProject);
                }else{
                    editorAnnotator.getFileObjects(currentProject).forEach(fileObject -> {
                        if(SonarLintScanner.isExcludedFile(preferences, fileObject)) {
                            editorAnnotator.cleanEditorAnnotations(fileObject);
                        }
                    });
                    WindowManager.getDefault().getRegistry().getOpened().forEach(topComponent -> 
                        FileOpenedNotifier.getFileObject(topComponent).ifPresent(fileObject -> {
                            try{
                                editorAnnotator.getEditorCookie(fileObject).ifPresent(editorCookie -> {
                                    try {
                                        SonarLintScanner.of(fileObject).ifPresent(scanner -> scanner.runAsync());
                                    } catch (IOException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                });
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                    );
                }
            }
        });
        /* Load current properties*/
        component.setSonarLintActive(sonarLintActivePreference.getValue());
        component.setExcludedFilePatterns(excludedFilePatternsPreference.getValue());
        component.setProjectKey(sonarLintProjectKeyPreference.getValue());
       
        return component;
    }
    
    @ProjectCustomizer.CompositeCategoryProvider.Registrations({
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-java-j2seproject"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-web-project"),
        @ProjectCustomizer.CompositeCategoryProvider.Registration(projectType = "org-netbeans-modules-maven")
    })
    public static SonarLintCustomizerTab createPropertiesComponent() {
        return new SonarLintCustomizerTab("SonarLint");
    }
    
}
