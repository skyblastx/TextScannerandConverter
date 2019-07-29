package com.example.textscannerandconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnSuccessListener;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class TextViewingActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    EditText textEdit;
    String fileName = "";

    private NavigationView mNavigationView;
    private View mHeaderView;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private TextView usernameView, emailView;
    private ImageView profilePicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_viewing);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        textEdit = (EditText)findViewById(R.id.text_edit);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mHeaderView = mNavigationView.getHeaderView(0);

        usernameView = (TextView) mHeaderView.findViewById(R.id.usernameView);
        emailView = (TextView) mHeaderView.findViewById(R.id.emailView);
        profilePicView = (ImageView) mHeaderView.findViewById(R.id.profilePicView);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

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

        Intent intent = getIntent();

        if(intent != null) {
            fileName = intent.getStringExtra("Name");
        }

        textEdit.setText(readText(fileName));
    }

    private String readText(String fileName) {
        File file = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter/" + fileName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

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
        getMenuInflater().inflate(R.menu.text_viewing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveText();
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveText() {
        //check whether a folder already exists
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(textEdit.getText() != null && fileName != null) {

            final File file = new File(root, fileName);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                //pdfDocument.writeTo(fileOutputStream);
                fileOutputStream.write(textEdit.getText().toString().getBytes());
                Toast.makeText(this, "Successfully overwritten", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(TextViewingActivity.this, MainActivity.class));
        } else if (id == R.id.nav_account_page) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
