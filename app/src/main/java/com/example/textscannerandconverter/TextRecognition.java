package com.example.textscannerandconverter;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
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
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextRecognition extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    EditText resultET, nameOfPDF;
    LinearLayout myRoot;
    ImageView imageIV;
    CardView editCardView;
    Button btnConvert, btnEditText;

    private static final int SCANNER_REQUEST_CODE = 99;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String cameraPermission[], storagePermission[];
    private static int count = 1;

    Uri image_uri;

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
        setContentView(R.layout.activity_text_recognition);

        resultET = (EditText)findViewById(R.id.resultEt);
        nameOfPDF = (EditText)findViewById(R.id.nameOfPDF);
        myRoot = (LinearLayout)findViewById(R.id.my_root);
        imageIV = (ImageView)findViewById(R.id.imageIv);
        editCardView = (CardView)findViewById(R.id.edit_card_view);
        btnConvert = (Button)findViewById(R.id.btnConvert);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mHeaderView = mNavigationView.getHeaderView(0);

        usernameView = (TextView) mHeaderView.findViewById(R.id.usernameView);
        emailView = (TextView) mHeaderView.findViewById(R.id.emailView);
        profilePicView = (ImageView) mHeaderView.findViewById(R.id.profilePicView);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        //camera permission
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

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

        if(intent != null){
            String message = intent.getStringExtra("MainActivity");
            if(message.equals("New")){
                showImageImportDialog();
            }
        }

        //convert text from image after pressing button
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //editCardView.setVisibility(View.VISIBLE);
                if(resultET.getText().toString().equals("")){
                    if(imageIV.getDrawable() != null){
                        Bitmap bitmap = ((BitmapDrawable) imageIV.getDrawable()).getBitmap();

                        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                        if(!recognizer.isOperational()){
                            Toast.makeText(view.getContext(),"Error", Toast.LENGTH_SHORT).show();
                        }else {
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> items = recognizer.detect(frame);
                            StringBuilder sb = new StringBuilder();
                            //transfer text from items to sb till there is no more text
                            for (int i = 0; i < items.size(); i++) {
                                TextBlock myItems = items.valueAt(i);
                                sb.append(myItems.getValue());
                                sb.append("\n");
                            }
                            //set text to edit text
                            if(sb.toString().equals("")){
                                resultET.setText("There is no text");
                            }else{
                                resultET.setText(sb.toString());
                            }
                        }
                        Intent intent = new Intent(TextRecognition.this, Pop.class);
                        intent.putExtra("ocr", resultET.getText().toString());
                        startActivityForResult(intent, 500);
                    }else {
                        Toast.makeText(view.getContext(),"There is no image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Intent intent = new Intent(TextRecognition.this, Pop.class);
                    intent.putExtra("ocr", resultET.getText().toString());
                    startActivityForResult(intent, 500);
                }
                btnConvert.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnConvert.setVisibility(View.VISIBLE);
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
        getMenuInflater().inflate(R.menu.text_recognition, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.save_image_text) {
            getNameFromDialogImageText();
        } else if (id == R.id.save_image) {
            getNameFromDialogImage();
        } else if (id == R.id.save_text) {
            getNameFromDialogText();
        }

        return super.onOptionsItemSelected(item);
    }

    private void getNameFromDialogImageText() {
        //Inflate dialog view
        final LayoutInflater[] li = {LayoutInflater.from(this)};
        View promptsView = li[0].inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

        //set dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (userInput.getText().toString().equals("")) {
                            nameOfPDF.setText("PDF" + count);
                            count++;
                        } else {
                            nameOfPDF.setText(userInput.getText());
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        //create the dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        //show dialog
        alertDialog.show();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                saveImageText();
            }
        });
    }

    private void saveImageText() {
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(resultET.getText() != null && nameOfPDF != null && imageIV.getDrawable() != null) {
            String txtName, pdfName;
            txtName = pdfName = nameOfPDF.getText().toString();

            if(!txtName.contains(".txt")){
                txtName += ".txt";
            }

            if(!pdfName.contains(".pdf")){
                pdfName += ".pdf";
            }

            final File txtFile = new File(root, txtName);
            final File pdfFile = new File(root, pdfName);

            if(txtFile.exists() && pdfFile.exists()) {
                final boolean[] choice = {true};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                alertDialog.setTitle("Files Already Exist")
                        .setMessage("Do you want to overwrite both existing files?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog ow = alertDialog.create();

                ow.show();

                ow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (choice[0]) {
                            overwriteImageText();
                        }
                    }
                });
            } else if(txtFile.exists()) {
                final boolean[] choice = {true};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                alertDialog.setTitle("Text File Already Exist")
                        .setMessage("Do you want to overwrite the existing text file?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog ow = alertDialog.create();

                ow.show();

                ow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (choice[0]) {
                            overwriteText();
                        }
                    }
                });
            } else if(pdfFile.exists()) {
                final boolean[] choice = {true};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                alertDialog.setTitle("File Already Exist")
                        .setMessage("Do you want to overwrite the existing file?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog ow = alertDialog.create();

                ow.show();

                ow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(choice[0]){
                            overwriteImage();
                        }
                    }
                });
            } else {
                Bitmap bitmap = ((BitmapDrawable) imageIV.getDrawable()).getBitmap();

                final PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(),1).create();

                PdfDocument.Page page = pdfDocument.startPage(pi);
                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#FFFFFF"));
                canvas.drawPaint(paint);

                bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(),true);
                paint.setColor(Color.BLUE);
                canvas.drawBitmap(bitmap, 0, 0, null);

                pdfDocument.finishPage(page);

                try{
                    FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                    pdfDocument.writeTo(fileOutputStream);
                    Toast.makeText(this,"Successfully saved", Toast.LENGTH_SHORT).show();
                }catch(IOException e){
                    e.printStackTrace();
                }
                pdfDocument.close();

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(txtFile);
                    fileOutputStream.write(resultET.getText().toString().getBytes());
                    Toast.makeText(this, "Successfully saved", Toast.LENGTH_SHORT).show();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(TextRecognition.this, "Either no image is present or no text is converted", Toast.LENGTH_SHORT).show();
        }
    }

    private void overwriteImageText() {
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(resultET.getText() != null && nameOfPDF != null && imageIV.getDrawable() != null) {
            String txtName, pdfName;
            txtName = pdfName = nameOfPDF.getText().toString();

            if(!txtName.contains(".txt")){
                txtName += ".txt";
            }

            if(!pdfName.contains(".pdf")){
                pdfName += ".pdf";
            }

            final File txtFile = new File(root, txtName);
            final File pdfFile = new File(root, pdfName);

            Bitmap bitmap = ((BitmapDrawable) imageIV.getDrawable()).getBitmap();

            final PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(),1).create();

            PdfDocument.Page page = pdfDocument.startPage(pi);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#FFFFFF"));
            canvas.drawPaint(paint);

            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(),true);
            paint.setColor(Color.BLUE);
            canvas.drawBitmap(bitmap, 0, 0, null);

            pdfDocument.finishPage(page);

            try{
                FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                pdfDocument.writeTo(fileOutputStream);
                Toast.makeText(this,"Successfully saved", Toast.LENGTH_SHORT).show();
            }catch(IOException e){
                e.printStackTrace();
            }
            pdfDocument.close();

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(txtFile);
                fileOutputStream.write(resultET.getText().toString().getBytes());
                Toast.makeText(this, "Successfully saved", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getNameFromDialogText() {
        //Inflate dialog view
        final LayoutInflater[] li = {LayoutInflater.from(this)};
        View promptsView = li[0].inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

        //set dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (userInput.getText().toString().equals("")) {
                            nameOfPDF.setText("PDF" + count);
                            count++;
                        } else {
                            nameOfPDF.setText(userInput.getText());
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        //create the dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        //show dialog
        alertDialog.show();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                saveText();
            }
        });
    }

    private void saveText() {
        //check whether a folder already exists
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(resultET.getText() != null && nameOfPDF != null) {
            String txtName = nameOfPDF.getText().toString();
            if(!txtName.contains(".txt")){
                txtName += ".txt";
            }

            final File file = new File(root, txtName);
            if (file.exists()) {
                final boolean[] choice = {true};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                alertDialog.setTitle("File Already Exist")
                        .setMessage("Do you want to overwrite the existing file?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog ow = alertDialog.create();

                ow.show();

                ow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (choice[0]) {
                            overwriteText();
                        }
                    }
                });
            } else {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(resultET.getText().toString().getBytes());
                    Toast.makeText(this, "Successfully saved", Toast.LENGTH_SHORT).show();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(TextRecognition.this, "No text converted", Toast.LENGTH_SHORT).show();
        }
    }

    private void overwriteText() {
        //check whether a folder already exists
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(resultET.getText() != null && nameOfPDF != null) {
            String txtName = nameOfPDF.getText().toString();
            if(!txtName.contains(".txt")){
                txtName += ".txt";
            }

            final File file = new File(root, txtName);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(resultET.getText().toString().getBytes());
                Toast.makeText(this, "Successfully overwritten", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getNameFromDialogImage() {
        //Inflate dialog view
        final LayoutInflater[] li = {LayoutInflater.from(this)};
        View promptsView = li[0].inflate(R.layout.prompts, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

        //set dialog message
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (userInput.getText().toString().equals("")) {
                            nameOfPDF.setText("PDF" + count);
                            count++;
                        } else {
                            nameOfPDF.setText(userInput.getText());
                        }
                        if(!nameOfPDF.getText().toString().contains(".pdf")){
                            nameOfPDF.append(".pdf");
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        //create the dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        //show dialog
        alertDialog.show();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                saveImage();
            }
        });
    }

    private void saveImage(){
        //check whether a folder already exists
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(imageIV.getDrawable() != null && nameOfPDF != null){
            String pdfName = nameOfPDF.getText().toString();
            if(!pdfName.contains(".pdf")){
                pdfName += ".pdf";
            }

            final File file = new File(root, pdfName);
            if(file.exists()){
                final boolean[] choice = {true};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

                alertDialog.setTitle("File Already Exist")
                        .setMessage("Do you want to overwrite the existing file?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = true;
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                choice[0] = false;
                                dialog.cancel();
                            }
                        });

                AlertDialog ow = alertDialog.create();

                ow.show();

                ow.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(choice[0]){
                            overwriteImage();
                        }
                    }
                });
            }else{
                Bitmap bitmap = ((BitmapDrawable) imageIV.getDrawable()).getBitmap();

                final PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(),1).create();

                PdfDocument.Page page = pdfDocument.startPage(pi);
                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#FFFFFF"));
                canvas.drawPaint(paint);

                bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(),true);
                paint.setColor(Color.BLUE);
                canvas.drawBitmap(bitmap, 0, 0, null);

                pdfDocument.finishPage(page);

                try{
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    pdfDocument.writeTo(fileOutputStream);
                    Toast.makeText(this,"Successfully saved", Toast.LENGTH_SHORT).show();
                }catch(IOException e){
                    e.printStackTrace();
                }

                pdfDocument.close();
            }
        }
    }

    private void overwriteImage() {
        //check whether a folder already exists
        File root = new File(Environment.getExternalStorageDirectory(), "Text Scanner and Converter");
        if(!root.exists()){
            root.mkdir();
        }

        if(imageIV.getDrawable() != null && nameOfPDF != null) {
            String PDFname = nameOfPDF.getText().toString();
            if(!PDFname.contains(".pdf")){
                PDFname += ".pdf";
            }
            Bitmap bitmap = ((BitmapDrawable) imageIV.getDrawable()).getBitmap();

            final PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();

            PdfDocument.Page page = pdfDocument.startPage(pi);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#FFFFFF"));
            canvas.drawPaint(paint);

            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
            paint.setColor(Color.BLUE);
            canvas.drawBitmap(bitmap, 0, 0, null);

            pdfDocument.finishPage(page);

            final File file = new File(root, PDFname);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                pdfDocument.writeTo(fileOutputStream);
                Toast.makeText(TextRecognition.this, "Successfully overwritten", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showImageImportDialog() {
        //items to display in dialog
        String[] items = {"Camera", "Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == 0){
                    //camera option clicked
                    if(!checkCameraPermission()){
                        //camera permission not allowed, request it
                        requestCameraPermission();
                    }else{
                        //permission allowed
                        pickCamera();
                    }
                }
                if(i == 1){
                    //gallery option clicked
                    if(!checkStoragePermission()){
                        //storage permission not allowed, request it
                        requestStoragePermission();
                    }else{
                        //permission allowed
                        pickGallery();
                    }
                }
            }
        });
        AlertDialog finalDialog = dialog.create();

        finalDialog.setCanceledOnTouchOutside(false);
        finalDialog.show(); //show dialog
    }

    private void pickGallery() {
        //codes for cropping
        /*//intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);*/

        //codes for document scanner
        int preference = ScanConstants.OPEN_MEDIA;
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        startActivityForResult(intent, SCANNER_REQUEST_CODE);
    }

    private void pickCamera() {
        //codes for cropping
        /*//intent to take picture with camera, will also save to storage to get hd image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic"); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image to Text"); //description
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);*/

        //codes for document scanner
        int preference = ScanConstants.OPEN_CAMERA;
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        startActivityForResult(intent, SCANNER_REQUEST_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return storagePermission;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return cameraPermission && storagePermission;
    }

    //handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case STORAGE_REQUEST_CODE:
                if(grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(writeStorageAccepted){
                        pickGallery();
                    }else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

    }

    //handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //codes for cropping
        /*if(resultCode == RESULT_OK) {
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //got image from storage, now crop it
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //got image from camera now crop it
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            //get cropped image
            if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if(resultCode == RESULT_OK){
                    Uri resultUri = result.getUri();
                    //set image to image view
                    mPreviewIv.setImageUri(resultUri);
                    //get drawable bitmap for text recognition
                    BitmapDrawable bitmapDrawable = (BitmapDrawable)mPreviewIv.getDrawable();
                    Bitmap bitmap = bitmapDrawable.getBitmap();

                    TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                    if(!recognizer.isOperational()){
                        Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show();
                    }else{
                        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                        SparseArray<TextBlock> items = recognizer.detect(frame);
                        StringBuilder sb = new StringBuilder();
                        //transfer text from items to sb till there is no more text
                        for(int i=0; i<items.size(); i++){
                            TextBlock myItems = items.valueAt(i);
                            sb.append(myItems.getValue());
                            sb.append("\n");
                        }
                        //set text to edit text
                        mResultEt.setText(sb.toString());
                    }
                }
            }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                //if there is an error show it
                Exception error = result.getError();
                Toast.makeText(this, ""+error, Toast.LENGTH_SHORT).show();
            }
        }*/

        //codes for document scanner
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 99) {
                Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    getContentResolver().delete(uri, null, null);
                    imageIV.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (requestCode == 500) {
                resultET.setText(data.getStringExtra("ocrResult"));
            }
        }
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
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
