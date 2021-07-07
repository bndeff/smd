package app.smd;

import java.util.ArrayList;

public class ProjectList {

    private static class Project {
        StateMachine stateMachine;
        String repr;
        String thumb;
        String name;
    }

    private final ArrayList<Project> projects;
    private int selIndex;
    private OnChangeListener onChangeListener;

    public ProjectList() {
        projects = new ArrayList<>();
        selIndex = -1;
        onChangeListener = null;
    }

    public void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void fireOnChange() {
        if(onChangeListener != null) onChangeListener.onChange();
    }

    private Project createProject(StateMachine sm) {
        Project p = new Project();
        p.stateMachine = sm;
        p.repr = sm.getRepresentation();
        p.thumb = sm.getThumbnail();
        p.name = sm.getName();
        sm.setOnChangeListener(() -> {
            p.repr = p.stateMachine.getRepresentation();
            p.thumb = p.stateMachine.getThumbnail();
            p.name = p.stateMachine.getName();
            fireOnChange();
        });
        return p;
    }

    public int getProjectCount() {
        return projects.size();
    }

    public void selectProject(int index) {
        if(index >= 0 && index < projects.size()) {
            selIndex = index;
        }
    }

    public int getSelIndex() {
        return selIndex;
    }

    public StateMachine getMachine() {
        if(selIndex < 0) return null;
        return projects.get(selIndex).stateMachine;
    }

    public void addProject(String name) {
        int newSel = selIndex + 1;
        StateMachine sm = new StateMachine();
        sm.setName(name);
        projects.add(newSel, createProject(sm));
        selIndex = newSel;
        fireOnChange();
    }

    public void cloneProject(String name) {
        if(selIndex < 0) return;
        int newSel = selIndex + 1;
        StateMachine sm = new StateMachine(projects.get(selIndex).stateMachine, false);
        sm.setName(name);
        projects.add(newSel, createProject(sm));
        selIndex = newSel;
        fireOnChange();
    }

    public void importProject(String name, String programOrRepr) {
        int newSel = selIndex + 1;
        StateMachine sm = new StateMachine(programOrRepr);
        if(name != null) sm.setName(name);
        projects.add(newSel, createProject(sm));
        selIndex = newSel;
        fireOnChange();
    }

    public void deleteProject() {
        projects.remove(selIndex);
        if(selIndex >= projects.size()) --selIndex;
        fireOnChange();
    }

    private String escapeRepr(String name) {
        // replace every ; with \;
        return name.replaceAll(";", "\\\\;");
    }

    private String unescapeRepr(String name) {
        // replace every \; with ;
        return name.replaceAll("\\\\;", ";");
    }

    public String exportAll() {
        StringBuilder sb = new StringBuilder();
        for(Project p : projects) {
            if (sb.length() > 0) sb.append(";");
            sb.append(escapeRepr(p.repr));
        }
        return sb.toString();
    }

    public void importAll(String fullRepr, boolean clear) {
        if(clear) {
            projects.clear();
            selIndex = -1;
        }
        for(String escapedRepr : fullRepr.split("(?<!\\\\);")) {
            // split by ; but not by \;
            selIndex += 1;
            projects.add(selIndex, createProject(new StateMachine(unescapeRepr(escapedRepr))));
        }
        fireOnChange();
    }

}