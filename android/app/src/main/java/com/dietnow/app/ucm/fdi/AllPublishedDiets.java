package com.dietnow.app.ucm.fdi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;

import com.dietnow.app.ucm.fdi.adapters.AllUsersAdapter;
import com.dietnow.app.ucm.fdi.adapters.MyDietsAdapter;
import com.dietnow.app.ucm.fdi.adapters.PublishedDietAdapter;
import com.dietnow.app.ucm.fdi.model.diet.Diet;
import com.dietnow.app.ucm.fdi.model.user.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AllPublishedDiets extends AppCompatActivity implements SearchView.OnQueryTextListener{

    private FirebaseAuth auth;
    private SearchView searchDiet;
    private PublishedDietAdapter PublishedDietAdapter;
    private androidx.recyclerview.widget.RecyclerView RecyclerView;
    private DatabaseReference bd;
    private ArrayList<Diet> dietList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //----------------Variables-------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_published_diets);
        auth                    = FirebaseAuth.getInstance();
        searchDiet              = findViewById(R.id.searchPublishedDiet);
        RecyclerView            = findViewById(R.id.PublishedDietRecycler);
        bd                      = FirebaseDatabase.getInstance(MainActivity.FIREBASE_DB_URL).getReference();
        dietList                = new ArrayList<Diet> ();

        RecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //----------------Fin Variables----------------

        //----------------Lista de Dietas------------------------
        getDiet();
        //----------------Fin Lista de Dietas--------------------

        //--------------------search--------------------------
        searchDiet.setOnQueryTextListener(this);
        //------------------Fin search------------------------

    }
    private void getDiet(){
        bd.child("diets").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String titulo = ds.child("title").getValue().toString();
                    int visit = ds.child("visits").getValue(int.class);
                    int like = ds.child("likes").getValue(int.class);
                    Boolean active = ds.child("active").getValue(Boolean.class);
                    String descripcion = ds.child("description").getValue().toString();
                    boolean published = ds.child("published").getValue(Boolean.class);
                    Diet us = new Diet(  descripcion, titulo , visit,  like);
                    us.setId(ds.child("id").getValue().toString());
                    if( active && published) {
                        dietList.add(us);
                    }
                }
                PublishedDietAdapter = new PublishedDietAdapter(dietList,AllPublishedDiets.this);
                RecyclerView.setAdapter(PublishedDietAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        PublishedDietAdapter.filtrado(newText);

        return false;
    }

}