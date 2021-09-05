package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

public class ProjectListActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private ProjectListAdapter pla;
    private final Random rand = new Random();

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

    private void unimplemented() {
        Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

    private String genProjectName() {
        return String.format(Locale.US, "pr%03d", rand.nextInt(1000));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miPlayProject) {
            Intent intent = new Intent(this, SimulationActivity.class);
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
        else if(id == R.id.miOpenProject) {
            unimplemented();
        }
        else if(id == R.id.miSaveProject) {
            unimplemented();
        }
        else if(id == R.id.miShareProject) {
            unimplemented();
        }
        else if(id == R.id.miUploadProject) {
            unimplemented();
        }
        else if(id == R.id.miDownloadProject) {
            unimplemented();
        }
        else if(id == R.id.miDeleteProject) {
            pl.deleteProject();
            applyChanges();
        }
        return true;
    }
}