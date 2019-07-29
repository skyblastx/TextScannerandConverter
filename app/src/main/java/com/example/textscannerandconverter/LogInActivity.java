package com.example.textscannerandconverter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthResult;
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

public class LogInActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private EditText Name;
    private EditText Password;
    private Button Login;
    private TextView Info;
    private int counter =3;
    private TextView userRegistration;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private TextView forgotPassword, anonLogin;
    private NavigationView mNavigationView;
    private View mHeaderView;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private TextView usernameView, emailView;
    private ImageView profilePicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        Name = (EditText)findViewById(R.id.etName);
        Password = (EditText)findViewById(R.id.etUserPassword);
        Info = (TextView)findViewById(R.id.tvInfo);
        anonLogin = (TextView)findViewById(R.id.tvAnonLogin);
        Login = (Button)findViewById(R.id.btnLogin);
        userRegistration =(TextView)findViewById(R.id.tvRegister);
        forgotPassword =(TextView)findViewById(R.id.tvForgotPassword);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mHeaderView = mNavigationView.getHeaderView(0);

        usernameView = (TextView) mHeaderView.findViewById(R.id.usernameView);
        emailView = (TextView) mHeaderView.findViewById(R.id.emailView);
        profilePicView = (ImageView) mHeaderView.findViewById(R.id.profilePicView);

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        Info.setText("Number of Attempts Remaining: " + counter);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        /* Checks if user has login to the application// */
        FirebaseUser user = firebaseAuth.getCurrentUser();

        //Directs User to Registration Page:
        userRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LogInActivity.this,RegistrationActivity.class));
            }
        });

        System.out.println("The current user is: " + user);
        //Doesn't prompt user to enter credentials again:
        if(user != null){
            finish();
            startActivity(new Intent(LogInActivity.this,MainActivity.class));
        }

        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validate(Name.getText().toString(),Password.getText().toString());

            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LogInActivity.this,PasswordActivity.class));
            }
        });

        anonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LogInActivity.this, MainActivity.class));
            }
        });

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

    private void validate(String userName, final String userPassword){

        progressDialog.setMessage("Verifying Details");
        progressDialog.show();
        if(Patterns.EMAIL_ADDRESS.matcher(userName).matches()) {
            firebaseAuth.signInWithEmailAndPassword(userName,userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        progressDialog.dismiss();
                        //  Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        Name.setBackgroundColor(Color.parseColor("'#ffffff"));
                        Password.setBackgroundColor(Color.parseColor("#ffffff"));
                        checkEmailVerification();
                    }else{
                        progressDialog.dismiss();
                        Name.setBackgroundColor(Color.parseColor("#ff0000"));
                        Password.setBackgroundColor(Color.parseColor("#ff0000"));
                        counter--;
                        Info.setText("Number of Attempts Remaining: " + counter);
                        if(counter==0){
                            Login.setEnabled(false);
                        }
                    }
                }
            });
        } else {
            DatabaseReference myRef = firebaseDatabase.getReference();
            myRef.child("user_ids").child(userName).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String userEmail = dataSnapshot.getValue(String.class);
                    firebaseAuth.signInWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                progressDialog.dismiss();
                                //  Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                checkEmailVerification();
                            }else{
                                progressDialog.dismiss();
                                Toast.makeText(LogInActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                counter--;
                                Info.setText("Number of Attempts Remaining: " + counter);
                                if(counter==0){
                                    Login.setEnabled(false);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    //Verifies User Email
    private void checkEmailVerification(){
        FirebaseUser firebaseUser = firebaseAuth.getInstance().getCurrentUser();
        Boolean emailFlag = firebaseUser.isEmailVerified();

        if(emailFlag){
            finish();
            startActivity(new Intent(LogInActivity.this,MainActivity.class));
        }else{
            Toast.makeText(this, "Please Verify Your Email Address", Toast.LENGTH_SHORT).show();
            firebaseAuth.signOut();
        }
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
        getMenuInflater().inflate(R.menu.log_in, menu);
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
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_account_page) {
            if(FirebaseAuth.getInstance().getCurrentUser() != null) {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            } else {
                startActivity(this.getIntent());
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
