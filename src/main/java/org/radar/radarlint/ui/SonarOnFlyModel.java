/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.radar.radarlint.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.radar.radarlint.IssueAnnotation;
import org.radar.radarlint.Severity;

/**
 *
 * @author tonio
 */
public class SonarOnFlyModel extends AbstractTableModel {

    List<IssueAnnotation> data = new ArrayList<>();

    public void setData(List<IssueAnnotation> issues) {
        data.clear();
        data.addAll(issues);
        fireTableDataChanged();
    }
    
    public IssueAnnotation getDataAt(int row){
        return data.get(row);
    }

    @Override
    public int getColumnCount() {
        return 5;
    }
    

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 1:
                return "message";
            case 0:
                return "level";
            case 2:
                return "type";
            case 3:
                return "key";
            case 4:
                return "rule name";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getValueAt(0, columnIndex).getClass();
        //return super.getColumnClass(columnIndex);
    }
    
    Severity getIconSeverity(IssueAnnotation annotation){
        switch (annotation.getIssue().getSeverity()) {
            default:
                return Severity.valueOf(annotation.getIssue().getSeverity());
        }
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        IssueAnnotation a = data.get(rowIndex);
        switch (columnIndex) {
            case 1:
                return a.getIssue().getMessage();
            case 0:
                return getIconSeverity(a);//.getIssue().getSeverity();
            case 2:
                return a.getIssue().getType();
            case 3:
                return "<html><u>" + a.getIssue().getRuleKey() + "</u></html>s";
            case 4:
                return a.getIssue().getRuleName();
            default:
                throw new IllegalArgumentException();
        }
    }
}
