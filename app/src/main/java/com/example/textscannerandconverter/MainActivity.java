package com.example.textscannerandconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /*public static final Map<String, Integer> ITEM_MAP = new HashMap<String, Integer>();
    public static int keyNumber = 1;*/

    private NavigationView mNavigationView;
    private View mHeaderView;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private TextView usernameView, emailView;
    private ImageView profilePicView;

    private RecyclerView PDFView;
    private Button pdfName;
    private ArrayList<PDF> pdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mHeaderView = mNavigationView.getHeaderView(0);
        PDFView = (RecyclerView) findViewById(R.id.PDFView);
        pdfName = (Button) findViewById(R.id.pdf_name);

        usernameView = (TextView) mHeaderView.findViewById(R.id.usernameView);
        emailView = (TextView) mHeaderView.findViewById(R.id.emailView);
        profilePicView = (ImageView) mHeaderView.findViewById(R.id.profilePicView);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(),TextRecognition.class);
                intent.putExtra("MainActivity","New");
                startActivity(intent);
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null) {
            DatabaseReference databaseReference = firebaseDatabase.getReference(user.getUid());
            StorageReference storageReference = firebaseStorage.getReference();

            storageReference.child(user.getUid()).child("Images/Profile Picture").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Retrieving URI
                    Picasso.get().load(uri).fit().centerCrop().into(profilePicView);
                }
            });

            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    usernameView.setText(userProfile.getUserName());
                    emailView.setText(userProfile.getUserEmail());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Toast.makeText(MainActivity.this,databaseError.getCode(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        File root = new File(Environment.getExternalStorageDirectory(),"Text Scanner and Converter");
        createList(root);
    }

    private void createList(File dir) {
        File fileList[] = dir.listFiles();

        if (fileList != null) {
            String[] filePath = new String[fileList.length];
            String[] fileName = new String[fileList.length];
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    //check to see if this is a directory
                    createList(fileList[i]);
                } else {
                    if (fileList[i].getName().contains(".pdf") || fileList[i].getName().contains(".txt")) {
                        fileName[i] = fileList[i].getName();
                        filePath[i] = fileList[i].getAbsolutePath();
                    }
                }
            }
            pdf = PDF.createPDFList(fileList.length, fileName, filePath);
            PdfAdapter adapter = new PdfAdapter(pdf);
            PDFView.setAdapter(adapter);
            PDFView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    /*private void addIntoView(File file) {
        //create a new textview to show the file
        TextView textView = new TextView(this);
        GridView.LayoutParams lp = new GridView.LayoutParams(1000, 200);
        textView.setLayoutParams(lp);
        textView.setElevation(3);
        textView.setText(file.getName());

        //giving the textview an id to use the textview later
        int id = View.generateViewId();
        textView.setId(id);
        ITEM_MAP.put("key" + keyNumber, id);
        keyNumber++;

        PDFView.addView(textView);
    }*/

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(this.getIntent());
        } else if (id == R.id.nav_account_page) {
            if(firebaseAuth.getCurrentUser() != null) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, LogInActivity.class);
                startActivity(intent);
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
