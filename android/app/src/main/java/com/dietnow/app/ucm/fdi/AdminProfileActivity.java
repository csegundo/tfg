package com.dietnow.app.ucm.fdi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.anychart.AnyChartView;

import com.dietnow.app.ucm.fdi.model.user.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * UserProfileActivity - Establece el perfil del usuario en la aplicación
 */
public class AdminProfileActivity extends AppCompatActivity {

    // Necesario para saber cuando el usuario ya ha elegido una imagen de la galeria
    private static final Integer PICK_IMAGE = 1;

    private TextView name, age;
    private Button settings;
    private ImageView image;
    private Uri filePath;
    private Button change;
    private String uid;
    private DatabaseReference db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private StorageReference storageRef, imagesRef;
    private AnyChartView steps;
    private AnyChartView weights;
    private Button addStepsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.dietnow.app.ucm.fdi.R.layout.activity_admin_profile);

        // inicializar los elementos
        settings   = findViewById(com.dietnow.app.ucm.fdi.R.id.settings);
        name       = findViewById(com.dietnow.app.ucm.fdi.R.id.profileName);
        age        = findViewById(com.dietnow.app.ucm.fdi.R.id.profileAge);
        image      = (ImageView) findViewById(com.dietnow.app.ucm.fdi.R.id.profileImage);
        change     = findViewById(com.dietnow.app.ucm.fdi.R.id.profileChangeImg);
        //addStepsBtn= findViewById(com.dietnow.app.ucm.fdi.R.id.addStepsBtn);



        // inicializar Google Firebase
        auth       = FirebaseAuth.getInstance();
        db         = FirebaseDatabase.getInstance(MainActivity.FIREBASE_DB_URL).getReference();

        /*
         * Para acceder a imagenes es con imagesRef.child("fileName")
         * Propiedades de las referencias: getPath(), getName() y getBucket()
         */
        storageRef = FirebaseStorage.getInstance().getReference(); // crear una instancia a la referencia del almacenamiento
        imagesRef  = storageRef.child("images"); // referencia exclusivamente para imagenes (nivel mas bajo)

        // llamada a Firebase para obtener la info del usuario logueado y actualizar la vista
        FirebaseUser currentUser = auth.getCurrentUser();
        ValueEventListener profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.child("users").child(currentUser.getUid()).getValue(User.class);
                Log.d("ES EL USUARIO: ",currentUser.toString());
                uid= currentUser.getUid();
                name.setText(user.getName().trim() + " " + user.getLastname().trim());
                age.setText(user.getAge() + " " + age.getText().toString());

                String fileName = "profile_" + currentUser.getUid() + ".jpg";
                storageRef.child("images/" + fileName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_LONG).show();
                        Executor executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream in = new java.net.URL(uri.toString()).openStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                                    handler.post(new Runnable() { // making changes in UI
                                        @Override
                                        public void run() {
                                            image.setImageBitmap(bitmap);
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    image.setImageResource(com.dietnow.app.ucm.fdi.R.drawable.ic_person_128_black); // imagen predeterminada
                                }
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        image.setImageResource(com.dietnow.app.ucm.fdi.R.drawable.ic_person_128_black); // imagen predeterminada
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("UserProfile", "UserProfile:onCancelled", databaseError.toException());
            }
        };
        db.addValueEventListener(profileListener);

        // Cambiar la imagen del usuario: gallery/docs
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageFromPhone();
            }
        });

        // Acciones del perfil
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdminProfileActivity.this);
                builder.setTitle(com.dietnow.app.ucm.fdi.R.string.profile_settings)
                        .setItems(com.dietnow.app.ucm.fdi.R.array.profile_settings_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case 0: logout(); break;
                                    case 1: editProfile(); break;
                                    case 2: deleteProfile(); break;
                                    default: break;
                                }
                            }
                        })
                        .setNegativeButton(com.dietnow.app.ucm.fdi.R.string.delete_alert_no_opt, null).show();
            }
        });

    }


    // this function is triggered when user selects the image from the imageChooser
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if(data == null){
                return;
            }
            try{
                // proceso explicativo https://www.youtube.com/watch?v=ZmgncLHk_s4
                filePath = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                image.setImageBitmap(bitmap); // actualizar la imagen directamente a la vista
                uploadImage(); // subir la imagen a Google Firebase Storage
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    /*
     * Sube una imagen a Google Firebase Storage
     * Si ya tiene subida una imagen y se sube otra se sobreescribe
     */
    private void uploadImage(){
        if(filePath != null){
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle(getResources().getString(com.dietnow.app.ucm.fdi.R.string.uploading));
            dialog.show();

            FirebaseUser currentUser = auth.getCurrentUser();

            // Se crea una referencia a la ruta de acceso completa del archivo
            String fileName = "profile_" + currentUser.getUid() + ".jpg";
            StorageReference userProfile = storageRef.child("images/" + fileName);

            // Se sube la imagen
            userProfile.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), getResources().getString(com.dietnow.app.ucm.fdi.R.string.uploaded_final), Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    // trackear el proceso de subida de la imagen
                    Double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    dialog.setMessage(progress.toString() + "% " + getResources().getString(com.dietnow.app.ucm.fdi.R.string.uploaded_progress));
                }
            });
        } else{
            Toast.makeText(getApplicationContext(), getResources().getString(com.dietnow.app.ucm.fdi.R.string.select_image_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void getImageFromPhone(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("return-data", true);
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(intent, getResources().getString(com.dietnow.app.ucm.fdi.R.string.select_image));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    private void deleteProfile() {
        FirebaseUser user = auth.getCurrentUser();

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // borrar de auth
                FirebaseUser userAuth = FirebaseAuth.getInstance().getCurrentUser();

                if (userAuth != null) {
                    Toast.makeText(getApplicationContext(),
                            "Usuario borrado correctamente",
                            Toast.LENGTH_LONG).show();

                    // borrar de la base de datos
                    User user = dataSnapshot.child("users").child(userAuth.getUid()).getValue(User.class);
                    user.setActive(false);
                    Map<String, Object> userValues = user.toMap();
                    db.child("users").child(userAuth.getUid()).updateChildren(userValues);

                    // redirige a la página principal
                    // auth.signOut();
                    Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
                    startActivity(intent);


                } else {
                    Toast.makeText(getApplicationContext(),
                            "El usuario no se ha borrado correctamente",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("TAG", "UserPost:onCancelled", databaseError.toException());
            }
        };
        db.addValueEventListener(postListener);
    }

    private void logout(){
        auth.signOut();
        Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void editProfile(){
        Intent intent = new Intent(AdminProfileActivity.this, UserProfileEditActivity.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
    }

}