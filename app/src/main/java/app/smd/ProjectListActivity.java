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

public class ProjectListActivity extends AppCompatActivity {

    private PersistedProjectList pl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        pl = new PersistedProjectList(this);

        RecyclerView rv = (RecyclerView) findViewById(R.id.listProjects);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        ProjectListAdapter pla = new ProjectListAdapter(pl.getWrappedProjectList());
        rv.setAdapter(pla);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_project_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miEditProject) {
            Intent intent = new Intent(this, StateListActivity.class);
            this.startActivity(intent);
        }
        return true;
    }
}