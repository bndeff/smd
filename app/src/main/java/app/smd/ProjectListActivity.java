package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.annotation.SuppressLint;
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

        RecyclerView rv = findViewById(R.id.listProjects);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        pla = new ProjectListAdapter(pl.getWrappedProjectList());
        rv.setAdapter(pla);
        SimpleItemAnimator rva = (SimpleItemAnimator) rv.getItemAnimator();
        if(rva != null) rva.setSupportsChangeAnimations(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    @SuppressLint("NotifyDataSetChanged")
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
            int oldIndex = pl.getSelIndex();
            pl.addProject(genProjectName());
            pl.persistState();
            pla.notifyItemChanged(oldIndex);
            pla.notifyItemInserted(pl.getSelIndex());
        }
        else if(id == R.id.miCloneProject) {
            int oldIndex = pl.getSelIndex();
            pl.cloneProject(genProjectName());
            pl.persistState();
            pla.notifyItemChanged(oldIndex);
            pla.notifyItemInserted(pl.getSelIndex());
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
        else if(id == R.id.miUploadProject) {
            Intent intent = new Intent(this, SerialActivity.class);
            this.startActivity(intent);
        }
        else if(id == R.id.miDeleteProject) {
            int oldIndex = pl.getSelIndex();
            pl.deleteProject();
            pl.persistState();
            pla.notifyItemRemoved(oldIndex);
            pla.notifyItemChanged(pl.getSelIndex());
        }
        return true;
    }
}