package app.smd;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistedProjectList {

    private final SharedPreferences sp;
    private final ProjectList pl;
    private OnChangeListener onChangeListener;

    public PersistedProjectList(Context context, String key) {
        sp = context.getSharedPreferences(key, Context.MODE_PRIVATE);
        pl = new ProjectList();
        loadState();
        onChangeListener = null;
        pl.setOnChangeListener(this::fireOnChange);
    }

    public PersistedProjectList(Context context) {
        this(context, "smd");
    }

    public void persistState() {
        SharedPreferences.Editor spe = sp.edit();
        spe.putString("projects", pl.exportAll());
        spe.putInt("selection", pl.getSelIndex());
        spe.apply();
    }

    private void loadState() {
        String fullRepr = sp.getString("projects", "");
        int selIndex = sp.getInt("selection", 0);
        pl.clear();
        if(fullRepr.isEmpty()) {
            pl.addProject("default");
        } else {
            pl.importAll(fullRepr);
        }
        pl.selectProject(selIndex);
    }

    public void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void fireOnChange() {
        if(onChangeListener != null) onChangeListener.onChange();
    }

    public int getProjectCount() {
        return pl.getProjectCount();
    }

    public void selectProject(int index) {
        pl.selectProject(index);
    }

    public int getSelIndex() {
        return pl.getSelIndex();
    }

    public StateMachine getMachine() {
        return pl.getMachine();
    }

    public void addProject(String name) {
        pl.addProject(name);
    }

    public void cloneProject(String name) {
        pl.cloneProject(name);
    }

    public void importProject(String name, String programOrRepr) {
        pl.importProject(name, programOrRepr);
    }

    public void deleteProject() {
        pl.deleteProject();
    }

    public String exportAll() {
        return pl.exportAll();
    }

    public void clear() {
        pl.clear();
    }

    public void importAll(String fullRepr) {
        pl.importAll(fullRepr);
    }

}
