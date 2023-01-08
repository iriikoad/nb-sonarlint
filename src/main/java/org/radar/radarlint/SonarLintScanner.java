package org.radar.radarlint;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.maven.model.Model;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;
import org.radar.radarlint.settings.ExcludedFilePatterns;
import org.radar.radarlint.settings.ServerUrlPreference;
import org.radar.radarlint.settings.SettingsAccessor;
import org.radar.radarlint.settings.SonarLintActivePreference;
import org.radar.radarlint.settings.SonarLintGlobalActivePreference;
import org.radar.radarlint.settings.SonarLintProjectKeyPreference;
import org.radar.radarlint.settings.TokenAccesor;
import org.radar.radarlint.ui.SonarLintPropertiesComponent;
import org.radar.radarlint.ui.SonarOnFlyTopComponent;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;
import org.sonarsource.sonarlint.core.client.api.common.ProgressMonitor;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.exceptions.StorageException;

/**
 *
 * @author Víctor
 */
public class SonarLintScanner implements Supplier<List<Issue>> {

    private static final Logger LOGGER = Logger.getLogger(SonarLintScanner.class.getName());
    private static final Executor executor = Executors.newSingleThreadExecutor();

    private static final String USER_AGENT = "SonarLint Netbeans";

    private final Language[] enabledLanguages = Language.values();
    private final FileObject fileObject;
    private final String text;
    private final String projectKey;

    private SonarLintScanner(FileObject fileObject, String content, String projectKey) {
        this.fileObject = fileObject;
        this.text = content;
        this.projectKey = projectKey;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public void runAsync() {
        LOGGER.log(Level.INFO, "perform on file object path: {0}", fileObject.getPath());

        EditorAnnotator.getInstance().cleanEditorAnnotations(fileObject);

        if (!new SonarLintGlobalActivePreference().getValue()) {
            LOGGER.log(Level.INFO, "Not activated");
            return;
        }
        CompletableFuture
                .supplyAsync(this, executor)
                .whenCompleteAsync((issues, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(Level.WARNING, throwable.toString(), throwable);
                    }
                });
    }

    @Override
    public List<Issue> get() {

        boolean connectOk = false;
        ProgressHandle handle = ProgressHandle.createHandle("SonarLint - Starting");
        SwingUtilities.invokeLater(() -> {
            handle.start();
            handle.switchToIndeterminate();
        });
        try {
            List<Issue> issues = new LinkedList<>();
            long startTime = System.currentTimeMillis();
            LinkedList<ClientInputFile> inputFiles = new LinkedList<>();

            ConnectedAnalysisConfiguration.Builder builder = ConnectedAnalysisConfiguration.builder();

            AtomicReference<String> projectKey = new AtomicReference();

            String projectKeyPref = this.projectKey;

            if (isSupportedLanguage(fileObject)) {
                LOGGER.log(Level.INFO, "{0} File object to analyze: {1}", new Object[]{(System.currentTimeMillis() - startTime) / 1000f, fileObject.getNameExt()});
                ClientInputFile clientInputFile = new ContentClientInputFile(fileObject, text, false);
                inputFiles.add(clientInputFile);
                Project pro = FileOwnerQuery.getOwner(fileObject);
                projectKey.set(getProjectKey(pro));
                builder.setBaseDir(Paths.get(pro.getProjectDirectory().getPath()));
            }

            if (!inputFiles.isEmpty()) {
                Map<String, String> extraProperties = new HashMap<>();

                
                ServerConfiguration.Builder serverConfigurationBuilder = ServerConfiguration.builder();
                serverConfigurationBuilder
                        .url(new ServerUrlPreference().getValue())
                        .userAgent(USER_AGENT);

                char[] token = new TokenAccesor().getValue();
                if (token != null) {
                    serverConfigurationBuilder.token(new String(token));
                    Arrays.fill(token, (char) 0);
                }

                ServerConfiguration serverConfiguration = serverConfigurationBuilder.build();
                ConnectedSonarLintEngine engine = SonarLintEngineFactory.getOrCreateEngine(enabledLanguages);

                GlobalStorageStatus gss = engine.getGlobalStorageStatus();
                if (gss == null || gss.getLastUpdateDate() == null
                        || engine.checkIfGlobalStorageNeedUpdate(serverConfiguration, new ProgressMonitor() {
                        }).needUpdate()) {
                    LOGGER.log(Level.INFO, "{0} Updating global", new Object[]{(System.currentTimeMillis() - startTime) / 1000f});
                    handle.setDisplayName("SonarLint - Updating global storage");
                    engine.update(serverConfiguration, new ProgressMonitor() {
                    });
                }

                connectOk = true;
                SonarOnFlyTopComponent.setStatus(SonarOnFlyTopComponent.STATUS.IN_PROGRESS);

                Map<String, RemoteProject> remoteProjects =  engine.downloadAllProjects(serverConfiguration, new ProgressMonitor() {
                        });
                for(RemoteProject rp : remoteProjects.values()){
                    LOGGER.log(Level.INFO, "Remote projects: {0} {1}", new Object[]{rp.getKey(), rp.getName()});
                }
                //find remote project
                RemoteProject rp = remoteProjects.values().stream().filter(p -> p.getKey().equals(projectKey.get())).findFirst()
                        .orElse(null);
                if (rp == null && !projectKeyPref.isEmpty()) {
                    rp = remoteProjects.values().stream().filter(p -> projectKeyPref.equals(p.getKey())).findFirst().orElse(null);
                }
                if(rp == null){
                    rp = remoteProjects.values().stream().filter(p -> projectKey.get().contains(p.getKey())).findFirst().orElse(null);
                }
                if (rp == null) {
                    // try better match
                    rp = remoteProjects.values().stream().filter(p -> projectKey.get().contains(p.getKey().split(":")[0]))
                            .findFirst()
                            .orElse(null);
                }
                if(rp == null){
                    LOGGER.log(Level.WARNING, "Remote project for {0} not found", new Object[] { projectKey.get() });
                    SonarOnFlyTopComponent.setProjects("", remoteProjects.values());
                    SonarOnFlyTopComponent.setStatus(SonarOnFlyTopComponent.STATUS.OK);
                    return List.of();
                }else{
                    projectKey.set(rp.getKey());
                    SonarOnFlyTopComponent.setProjects(rp.getKey(), remoteProjects.values());
                }
                ConnectedAnalysisConfiguration analysisConfig = builder
                        .setProjectKey(projectKey.get())
                        .addInputFiles(inputFiles)
                        .putAllExtraProperties(extraProperties)
                        .build();
                
                boolean projectStorageNeedsUpdate;
                try {
                    projectStorageNeedsUpdate = engine.checkIfProjectStorageNeedUpdate(serverConfiguration, projectKey.get(), new ProgressMonitor() {
                    }).needUpdate();
                } catch (StorageException ex) {
                    projectStorageNeedsUpdate = true;
                }
                if (projectStorageNeedsUpdate) {
                    LOGGER.log(Level.INFO, "{0} Updating project", new Object[]{(System.currentTimeMillis() - startTime) / 1000f});
                    handle.setDisplayName("SonarLint - Updating project storage");
                    engine.updateProject(serverConfiguration, projectKey.get(), new ProgressMonitor() {
                    });
                }
                LOGGER.log(Level.INFO, "{0} Start analisys", new Object[]{(System.currentTimeMillis() - startTime) / 1000f});
                handle.setDisplayName("SonarLint - Running analysis");
                engine.analyze(analysisConfig, (Issue issue) -> {
                    LOGGER.log(Level.INFO, "Issue: {0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}", new Object[]{issue.getInputFile().relativePath(), issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset(), issue.getSeverity(), issue.getRuleName(), issue.getType(), issue.getMessage()});
                    FileObject fileObject1 = issue.getInputFile().getClientObject();
                    boolean attached = EditorAnnotator.getInstance().tryToAttachAnnotation(issue, fileObject1);
                    issues.add(issue);
                }, (String string, LogOutput.Level level) -> {
                    LOGGER.log(Level.INFO, "{0} {1}", new Object[]{(System.currentTimeMillis() - startTime) / 1000f, string});
                }, new ProgressMonitor() {

                    @Override
                    public void setFraction(float fraction) {
                        LOGGER.log(Level.INFO, "{0} fraction: {1}", new Object[]{(System.currentTimeMillis() - startTime) / 1000f, fraction});
                    }

                    @Override
                    public void setMessage(String msg) {
                        LOGGER.log(Level.INFO, "{0} message: {1}", new Object[]{(System.currentTimeMillis() - startTime) / 1000f, msg});
                    }
                });

                SonarOnFlyTopComponent.setStatus(SonarOnFlyTopComponent.STATUS.OK);
            }
            return issues;
        } finally {
            SonarOnFlyTopComponent.setStatus(connectOk ? SonarOnFlyTopComponent.STATUS.OK : SonarOnFlyTopComponent.STATUS.NOT_CONNECTED);
            SwingUtilities.invokeLater(() -> {
                handle.finish();
            });
        }
    }

    private boolean isSupportedLanguage(FileObject fileObject) {
        boolean isEnabledLanguage = false;
        for (Language enabledLanguage : enabledLanguages) {
            if (fileObject.getExt().equals(enabledLanguage.getLanguageKey())) {
                isEnabledLanguage = true;
                break;
            }
        }
        return isEnabledLanguage;
    }

    public String getProjectKey(Project project) {
        MvnProjectAnalyzer mvnProjectAnalyzer = new MvnProjectAnalyzer();
        Model model = mvnProjectAnalyzer.createModel(project);
        if (model != null) {
            return mvnProjectAnalyzer.getProjectKey(model);
        } else {
            return ProjectUtils.getInformation(project).getName();
        }
    }

    private static boolean isScanningNeeded(FileObject fileObject) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        if (ownerProject == null) {
            return false;
        } else {
            Preferences preferences = ProjectUtils.getPreferences(ownerProject, SonarLintPropertiesComponent.class, false);
            SettingsAccessor<Boolean> sonarLintActivePreference = new SonarLintActivePreference(preferences);
            return sonarLintActivePreference.getValue() && !isExcludedFile(preferences, fileObject);
        }
    }

    private static String getProjectKeyPref(FileObject fileObject) {
        Project ownerProject = FileOwnerQuery.getOwner(fileObject);
        if (ownerProject == null) {
            return "";
        } else {
            Preferences preferences = ProjectUtils.getPreferences(ownerProject, SonarLintPropertiesComponent.class, false);
            SettingsAccessor<String> sonarLintProjectKeyPreference = new SonarLintProjectKeyPreference(preferences);
            return sonarLintProjectKeyPreference.getValue();
        }
    }

    public static boolean isExcludedFile(Preferences preferences, FileObject fileObject) {
        SettingsAccessor<String> excludedFilePatternsPreference = new ExcludedFilePatterns(preferences);
        String patterns[] = excludedFilePatternsPreference.getValue().split("\\s*,\\s*");
        for (String pattern : patterns) {
            if (Pattern.compile(pattern).matcher(fileObject.getNameExt()).matches()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<SonarLintScanner> of(Document document) throws BadLocationException {
        Source source = Source.create(document);
        FileObject fileObject = source.getFileObject();
        if (isScanningNeeded(fileObject)) {
            return Optional.of(new SonarLintScanner(fileObject, document.getText(0, document.getLength()), getProjectKeyPref(fileObject)));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<SonarLintScanner> of(FileObject fileObject) throws IOException {
        return of(fileObject, getProjectKeyPref(fileObject));
    }

    public static Optional<SonarLintScanner> of(FileObject fileObject, String projectKey) throws IOException {
        if (isScanningNeeded(fileObject)) {
            return Optional.of(new SonarLintScanner(fileObject, fileObject.asText(), projectKey));
        } else {
            return Optional.empty();
        }
    }

}
