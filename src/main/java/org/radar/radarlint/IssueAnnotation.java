package org.radar.radarlint;

import org.openide.text.Annotation;
import org.openide.text.Line;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

/**
 *
 * @author Victor
 */
public class IssueAnnotation extends Annotation {
    private final Issue issue;
    private final Severity severity;
    private final String description;
    private Runnable gotoLine;
    private Runnable updateLine;
    private Line line;

    public IssueAnnotation(Issue issue) {
        this.issue = issue;
        this.severity = Severity.valueOf(issue.getSeverity());
        this.description = severity + ": " + issue.getMessage();
    }

    public Runnable getGotoLine() {
        return gotoLine;
    }

    public void setGotoLine(Runnable gotoLine) {
        this.gotoLine = gotoLine;
    }

    public Runnable getUpdateLine() {
        return updateLine;
    }

    public void setUpdateLine(Runnable updateLine) {
        this.updateLine = updateLine;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Issue getIssue() {
        return issue;
    }

    @Override
    public String getAnnotationType() {
        return "sonarqube-" + severity.name().toLowerCase() + "-annotation";
    }

    @Override
    public String getShortDescription() {
        return description;
    }

}
