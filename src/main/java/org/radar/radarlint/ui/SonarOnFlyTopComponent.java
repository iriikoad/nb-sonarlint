/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/NetBeansModuleDevelopment-files/templateTopComponent637.java to edit this template
 */
package org.radar.radarlint.ui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.radar.radarlint.IssueAnnotation;
import org.radar.radarlint.Severity;
import org.radar.radarlint.SonarLintEngineFactory;
import org.radar.radarlint.SonarLintScanner;
import org.radar.radarlint.settings.SonarLintGlobalActivePreference;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.radar.radarlint.ui//SonarOnFlyTopComponent//EN", autostore = false)
@TopComponent.Description(preferredID = "SonarOnFlyTopComponent",
        // iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = true)
@ActionID(category = "Window", id = "org.radar.radarlint.ui.SonarOnFlyTopComponent")
@ActionReference(path = "Menu/Window" /* , position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_SonarOnFlyTopComponentAction", preferredID = "SonarOnFlyTopComponent")
@Messages({
        "CTL_SonarOnFlyTopComponentAction=Sonar On Fly",
        "CTL_SonarOnFlyTopComponent=Sonar On Fly Window",
        "HINT_SonarOnFlyTopComponent=This is a SonarOnFly window"
})
public final class SonarOnFlyTopComponent extends TopComponent {

    SonarOnFlyModel onFlyModel = new SonarOnFlyModel();
    Map<String, List<IssueAnnotation>> issuesMap = new ConcurrentHashMap<>();
    List<RemoteProject> projects = new ArrayList<>();
    FileObject fileObject;
    boolean inProgress = false;

    public enum STATUS {
        NOT_CONNECTED,
        IN_PROGRESS,
        OK
    }

    public SonarOnFlyTopComponent() {
        initComponents();
        setName("Sonar On Fly");

        for (Severity s : Severity.values()) {
            JCheckBox chck = new JCheckBox();
            JLabel label = new JLabel(s.getUserDescription());
            try {
                ImageIcon img = new ImageIcon(ImageIO.read(this.getClass().getResource(s.getResourcePath())));
                label.setIcon(img);
            } catch (Exception e) {
                // error?
            }
            chck.addActionListener(e -> {
                s.setVisible(chck.isSelected());
                onFlyModel.data.forEach(a -> {
                    a.getUpdateLine().run();
                });
            });
            chck.setSelected(s.isVisible());
            jPanelSevList.add(chck);
            jPanelSevList.add(label);
        }

        jCheckBoxOnOff.setSelected(new SonarLintGlobalActivePreference().getValue());

        jTable1.setModel(onFlyModel);

        jTable1.setDefaultRenderer(Severity.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                    int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Severity severity = (Severity) value;
                JLabel l = new JLabel(severity.getUserDescription());
                try {
                    ImageIcon img = new ImageIcon(ImageIO.read(this.getClass().getResource(severity.getResourcePath())));
                    l.setIcon(img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return l;
            }
        });

        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) { // to detect doble click events
                    JTable target = (JTable) me.getSource();
                    int row = target.getSelectedRow(); // select a row
                    onFlyModel.getDataAt(row).getGotoLine().run();
                }
                if (me.getClickCount() == 1) { // to detect doble click events
                    JTable target = (JTable) me.getSource();
                    int row = target.getSelectedRow(); // select a row
                    int col = target.getSelectedColumn();
                    if (col == 3) {
                        RuleDialog.showRule(WindowManager.getDefault().getMainWindow(),
                                SonarLintEngineFactory.getOrCreateEngine(Language.values())
                                        .getRuleDetails(onFlyModel.getDataAt(row).getIssue().getRuleKey()));
                    }
                }
            }
        });
    }

    public void updateData(FileObject fileObject, List<IssueAnnotation> issues) {
        this.fileObject = fileObject;
        if (issues != null) {
            issuesMap.put(fileObject.getPath(), issues);
        } else {
            issues = issuesMap.get(fileObject.getPath());
        }
        jLabelFilePath.setText(new File(fileObject.getPath()).getName());
        if (issues != null) {
            List<IssueAnnotation> issuesSort = new ArrayList<>(issues);
            issuesSort.sort(
                    (IssueAnnotation o1, IssueAnnotation o2) -> Integer.compare(o1.getSeverity().ordinal(), o2.getSeverity().ordinal()));
            onFlyModel.setData(issuesSort);
        } else {
            onFlyModel.setData(List.of());
        }
    }

    public void updateProjects(String currentProject, Collection<RemoteProject> projects) {
        inProgress = true;
        this.projects.clear();
        this.projects.addAll(projects);
        List<RemoteProject> keys = new ArrayList<>();
        RemoteProject empty = new RemoteProject() {
            @Override
            public String getKey() {
                return "";
            }

            @Override
            public String getName() {
                return "";
            }
        };
        keys.add(empty);
        keys.addAll(projects);
        RemoteProject rp = projects.stream().filter(p -> p.getKey().equals(currentProject)).findFirst().orElse(empty);
        jComboBoxProject.setModel(new javax.swing.DefaultComboBoxModel<>(keys.toArray(RemoteProject[]::new)));
        jComboBoxProject.getModel().setSelectedItem(rp);

        jComboBoxProject.setRenderer(new ListCellRenderer<RemoteProject>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends RemoteProject> list, RemoteProject p, int index,
                    boolean isSelected, boolean cellHasFocus) {
                if (p == null) {
                    return new JLabel("");
                }
                return new JLabel(p.getKey() + " - " + p.getName());
            }
        });
        inProgress = false;
    }

    private static Optional<SonarOnFlyTopComponent> getTopComponent() {
        return TopComponent.getRegistry().getOpened().stream().filter(t -> t instanceof SonarOnFlyTopComponent)
                .map(t -> (SonarOnFlyTopComponent) t)
                .findFirst();
    }

    public static void setData(FileObject fileObject, List<IssueAnnotation> issues) {
        Optional<SonarOnFlyTopComponent> ot = getTopComponent();
        if (ot.isPresent()) {
            SwingUtilities.invokeLater(() -> {
                ot.get().updateData(fileObject, issues);
            });
        }
    }

    public static void setStatus(STATUS status) {
        Optional<SonarOnFlyTopComponent> ot = getTopComponent();
        if (ot.isPresent()) {
            SwingUtilities.invokeLater(() -> {
                ot.get().jLabelStatus.setText(status.name());
            });
        }
    }

    public static void setProjects(String currentProject, Collection<RemoteProject> projects) {
        Optional<SonarOnFlyTopComponent> ot = getTopComponent();
        if (ot.isPresent()) {
            SwingUtilities.invokeLater(() -> {
                ot.get().updateProjects(currentProject, projects);
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jCheckBoxOnOff = new javax.swing.JCheckBox();
        jComboBoxProject = new javax.swing.JComboBox<>();
        jLabelStatus = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanelSevList = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabelFilePath = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jTable1.setModel(onFlyModel);
        jScrollPane1.setViewportView(jTable1);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        org.openide.awt.Mnemonics.setLocalizedText(jCheckBoxOnOff, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jCheckBoxOnOff.text")); // NOI18N
        jCheckBoxOnOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxOnOffActionPerformed(evt);
            }
        });

        jComboBoxProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxProjectActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabelStatus, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabelStatus.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabel4.text")); // NOI18N
        jPanelSevList.add(jLabel4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jCheckBoxOnOff)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelStatus)
                        .addGap(64, 64, 64)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxProject, 0, 260, Short.MAX_VALUE))
                    .addComponent(jPanelSevList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxProject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxOnOff)
                    .addComponent(jLabelStatus)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelSevList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        add(jPanel1, java.awt.BorderLayout.PAGE_START);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabel1.text")); // NOI18N
        jPanel2.add(jLabel1);

        org.openide.awt.Mnemonics.setLocalizedText(jLabelFilePath, org.openide.util.NbBundle.getMessage(SonarOnFlyTopComponent.class, "SonarOnFlyTopComponent.jLabelFilePath.text")); // NOI18N
        jLabelFilePath.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jPanel2.add(jLabelFilePath);

        add(jPanel2, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxOnOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxOnOffActionPerformed

        //save option
        new SonarLintGlobalActivePreference().setValue(jCheckBoxOnOff.isSelected());
        jComboBoxProjectActionPerformed(evt);
    }//GEN-LAST:event_jCheckBoxOnOffActionPerformed

    private void jComboBoxProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxProjectActionPerformed
        try {
            if(!inProgress && fileObject != null && jComboBoxProject.getSelectedItem() != null){
                SonarLintScanner.of(fileObject, ((RemoteProject)jComboBoxProject.getSelectedItem()).getKey()).ifPresent(s -> s.runAsync());
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_jComboBoxProjectActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBoxOnOff;
    private javax.swing.JComboBox<RemoteProject> jComboBoxProject;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelFilePath;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelSevList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

}
