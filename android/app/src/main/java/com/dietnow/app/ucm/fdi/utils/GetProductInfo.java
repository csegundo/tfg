package com.dietnow.app.ucm.fdi.utils;

import android.util.Log;

import com.dietnow.app.ucm.fdi.apis.OpenFoodFactsService;
import com.dietnow.app.ucm.fdi.model.diet.Aliment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Contiene la llamada a la API de Open Food Facts para obtener la info de un alimento
 */
public class GetProductInfo {

    private GetProductInfo instance;
    private Retrofit retrofit;

    public GetProductInfo(){
        retrofit = new Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/api/v0/product/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }

    public GetProductInfo getInstance(){
        if(instance == null){
            instance = new GetProductInfo();
        }
        return instance;
    }

    public Aliment getInfo(String barcode){
        Aliment product = null;

        if(!barcode.isEmpty()){
            OpenFoodFactsService api = retrofit.create(OpenFoodFactsService.class);
            Call<RetrofitResponse> request = api.getProductInfo(barcode);
            try{
                /*
                // TODO llamada a la API de los alimentos
                // Info sacada de: https://howtodoinjava.com/retrofit2/retrofit-sync-async-calls/

                // Basado en el ejemplo anterior -> ver que devuelve la api y crear una clase personalizada para la respuesta como
                // hicimos con el modulo de usuarios --> basicamente es cambiar la clase RetrofitResponse por otra XXXX adaptada a
                // la respuesta de la llamada a la API
                Response<RetrofitResponse> response = request.execute();
                RetrofitResponse apiResponse = response.body();

                // Convertir la respuesta en la clase Aliment
                */
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        return product;
    }
}