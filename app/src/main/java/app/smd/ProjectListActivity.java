package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.Locale;

public class ProjectListActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private ProjectListAdapter pla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        pl = new PersistedProjectList(this);

        RecyclerView rv = (RecyclerView) findViewById(R.id.listProjects);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        pla = new ProjectListAdapter(pl.getWrappedProjectList());
        rv.setAdapter(pla);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pl.loadState();
        pla.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_project_list, menu);
        return true;
    }

    private void applyChanges() {
        pl.persistState();
        pla.notifyDataSetChanged();
    }

    private String genProjectName() {
        return String.format(Locale.US, "Project #%d", pl.getProjectCount() + 1);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miPlayProject) {
            Intent intent = new Intent(this, SimulationActivity.class);
            intent.putExtra("mode", 0);
            this.startActivity(intent);
        }
        else if(id == R.id.miEditProject) {
            Intent intent = new Intent(this, StateListActivity.class);
            this.startActivity(intent);
        }
        else if(id == R.id.miNewProject) {
            pl.addProject(genProjectName());
            applyChanges();
        }
        else if(id == R.id.miCloneProject) {
            pl.cloneProject(genProjectName());
            applyChanges();
        }
        else if(id == R.id.miImportProject) {
            Intent intent = new Intent(this, DataActivity.class);
            intent.putExtra("mode", 0);
            this.startActivity(intent);
        }
        else if(id == R.id.miExportProject) {
            Intent intent = new Intent(this, DataActivity.class);
            intent.putExtra("mode", 1);
            this.startActivity(intent);
        }
        else if(id == R.id.miDeleteProject) {
            pl.deleteProject();
            applyChanges();
        }
        return true;
    }
}