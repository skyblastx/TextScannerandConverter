package com.example.textscannerandconverter;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ImageView profileUpdate, changePassword, profilePicture;
    private TextView profileName, profileEmail;
    private Button btnUpload, btnDownload;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private FirebaseUser user;
    private Animation atg,atgtwo,atgthree;
    private NavigationView mNavigationView;
    private View mHeaderView;
    private TextView usernameView, emailView;
    private ImageView profilePicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mHeaderView = mNavigationView.getHeaderView(0);

        usernameView = (TextView) mHeaderView.findViewById(R.id.usernameView);
        emailView = (TextView) mHeaderView.findViewById(R.id.emailView);
        profilePicView = (ImageView) mHeaderView.findViewById(R.id.profilePicView);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        profilePicture =(ImageView)findViewById(R.id.ivProfilePic);
        profileEmail =(TextView)findViewById(R.id.tvProfileEmail);
        profileName=(TextView)findViewById(R.id.tvProfileName);
        profileUpdate=(ImageView)findViewById(R.id.btnProfileUpdate);
        changePassword=(ImageView)findViewById(R.id.btnChangePassword);
        btnUpload = (Button)findViewById(R.id.btnUpload);
        btnDownload = (Button)findViewById(R.id.btnDownload);

        atg = AnimationUtils.loadAnimation(this, R.anim.atg);
        atgtwo = AnimationUtils.loadAnimation(this, R.anim.atgtwo);
        atgthree = AnimationUtils.loadAnimation(this, R.anim.atgthree);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        user = firebaseAuth.getCurrentUser();

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

        if(user != null) {
            DatabaseReference databaseReference = firebaseDatabase.getReference(firebaseAuth.getUid());

            final StorageReference storageReference = firebaseStorage.getReference();
            storageReference.child(firebaseAuth.getUid()).child("Images/Profile Picture").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Retrieving URI
                    Picasso.get().load(uri).fit().centerCrop().into(profilePicture);
                }
            });

            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                //Records any changes on database:
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    profileEmail.setText("Email: "+userProfile.getUserEmail());
                    profileName.setText("Name: "+userProfile.getUserName());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Toast.makeText(ProfileActivity.this,databaseError.getCode(),Toast.LENGTH_SHORT).show();
                }
            });

            profileUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(ProfileActivity.this,UpdateProfile.class));
                }
            });

            changePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(ProfileActivity.this,UpdatePassword.class));
                }
            });

            btnUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
                    upload(root);
                }
            });

            btnDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
                    download(root);
                }
            });
        }

        //set animation
        profilePicture.startAnimation(atg);
        profileEmail.startAnimation(atgtwo);
        profileName.startAnimation(atgtwo);
        profileUpdate.startAnimation(atgtwo);
        changePassword.startAnimation(atgthree);
    }

    private void upload(File root) {
        ArrayList<PDF> pdf = getAllFiles(root);
        ArrayList<String> pdfName = new ArrayList<>();
        DatabaseReference myRef = firebaseDatabase.getReference(user.getUid()).child("Files");
        StorageReference storageReference = firebaseStorage.getReference().child(user.getUid()).child("Text Scanner and Converter");

        for(int i = 0; i < pdf.size(); i++){
            StorageReference pdfRef = storageReference.child(pdf.get(i).getName());
            pdfName.add(pdf.get(i).getName());
            File file = new File(root, pdfName.get(i));
            Uri uri = Uri.fromFile(file);

            UploadTask uploadTask = pdfRef.putFile(uri);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(ProfileActivity.this, "Upload Successful!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ProfileActivity.this, "Upload Failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        myRef.push().setValue(pdfName);
    }

    private void download(final File root) {
        DatabaseReference myRef = firebaseDatabase.getReference(user.getUid()).child("Files");
        final StorageReference storageReference = firebaseStorage.getReference().child(user.getUid()).child("Text Scanner and Converter");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    for(DataSnapshot child : ds.getChildren()){
                        final String fileName = child.getValue(String.class);
                        //pdfName.add(temp);
                        StorageReference pdfRef = storageReference.child(fileName);

                        final File file = new File(root, fileName);
                        if(!file.exists()){
                            pdfRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    downloadFile(ProfileActivity.this, fileName, "Text Scanner and Converter", uri.toString());
                                    Toast.makeText(ProfileActivity.this, "Downloading", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ProfileActivity.this, "Fail to download files", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, databaseError.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFile(Context context, String filename, String destinationDirectory, String url) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(destinationDirectory, filename);

        downloadManager.enqueue(request);
    }

    private ArrayList<PDF> getAllFiles(File file) {
        ArrayList<PDF> pdf = null;
        File fileList[] = file.listFiles();

        if(fileList != null){
            String[] filePath = new String[fileList.length];
            String[] fileName = new String[fileList.length];
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    //check to see if this is a directory
                    getAllFiles(fileList[i]);
                } else {
                    if (fileList[i].getName().contains(".pdf") || fileList[i].getName().contains(".txt")) {
                        fileName[i] = fileList[i].getName();
                        filePath[i] = fileList[i].getAbsolutePath();
                    }
                }
            }
            pdf = PDF.createPDFList(fileList.length, fileName, filePath);
        }

        return pdf;
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
        getMenuInflater().inflate(R.menu.profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            if(user != null) {
                firebaseAuth.getInstance().signOut();
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            } else {
                Toast.makeText(ProfileActivity.this, "You are not logged in", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_account_page) {
            startActivity(this.getIntent());
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
