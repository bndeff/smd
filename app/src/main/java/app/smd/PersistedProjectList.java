package app.smd;

import static java.lang.Math.min;

import android.content.Context;
import android.content.SharedPreferences;

@SuppressWarnings("unused")
public class PersistedProjectList {

    private final SharedPreferences sp;
    private final ProjectList pl;
    private final Context savedContext;
    private OnChangeListener onChangeListener;

    public PersistedProjectList(Context context, String key) {
        savedContext = context;
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

    public void loadState() {
        String fullRepr = sp.getString("projects", "");
        int selIndex = sp.getInt("selection", 0);
        pl.clear();
        if(fullRepr.isEmpty()) {
            loadDemoProjects();
        } else {
            pl.importAll(fullRepr);
        }
        pl.selectProject(selIndex);
    }

    private void loadDemoProjects() {
        String[] demoProjects = savedContext.getResources().getStringArray(R.array.demo_projects);
        String[] demoProjectsTitles = savedContext.getResources().getStringArray(R.array.demo_project_titles);
        int demoCount = min(demoProjects.length, demoProjectsTitles.length);
        for(int i = 0; i < demoCount; ++i) {
            pl.importProject(demoProjectsTitles[i], demoProjects[i]);
        }
    }

    public void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void fireOnChange() {
        if(onChangeListener != null) onChangeListener.onChange();
    }

    public ProjectList getWrappedProjectList() {
        return pl;
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
