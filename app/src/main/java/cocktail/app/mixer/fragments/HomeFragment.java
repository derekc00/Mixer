package cocktail.app.mixer.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import cocktail.app.mixer.DetailActivity;
import cocktail.app.mixer.Drink;
import cocktail.app.mixer.R;
import cocktail.app.mixer.adapters.DrinkAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    public static final String RANDOM_DRINK_URL = "https://www.thecocktaildb.com/api/json/v1/1/random.php"; // Create string to hold http for API request
    public static final String TAG = "HomeFragment";    // Create a tag for logging this activity
    public static int numDrinks = 15;
    public static int refreshCounter = 2;

    private DrinkAdapter drinkAdapter;

    protected SwipeRefreshLayout swipeContainer;
    List<Drink> drinks = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        if(DetailActivity.getKey() == 1){
            getDrinkAdapter().notifyDataSetChanged();
        }

    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvDrinks = view.findViewById(R.id.rvDrinks);
        swipeContainer = view.findViewById(R.id.swipeContainer);

        // Create the adapter
        DrinkAdapter drinkAdapter = new DrinkAdapter(getContext(), drinks);
        setDrinkAdapter(drinkAdapter);

        // Set the adapter o the recycler view
        rvDrinks.setAdapter(drinkAdapter);

        // Set a layout manager
        rvDrinks.setLayoutManager(new LinearLayoutManager(getContext()));
        queryDrinks(drinkAdapter, "none");
        rvDrinks.setVisibility(View.VISIBLE);



        // Refresh screen when you swipe up
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // logic to show adds on every 3 refreshes
                refreshCounter += 1;
                if (refreshCounter%3 == 0){
                    generateAds();
                }
                else {
                    refreshDrinks();
                }
            }
        });
    }

    protected void queryDrinks(DrinkAdapter drinkAdapter, String Placeholder) {
        for (int i = 0; i < numDrinks; i++) {
            // Instantiate an AsyncHttpClient to execute the API request
            AsyncHttpClient client = new AsyncHttpClient();

            // Make a get request on the client object
            client.get(RANDOM_DRINK_URL, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    //Log.d(TAG, "Success"); // log for success, connected to TAG
                    JSONObject jsonObject = json.jsonObject; // we store the response jsonObject in the variable jsonObject
                    try {
                        JSONArray result = jsonObject.getJSONArray("drinks");
                        //Log.i(TAG, "Results: " + result.toString()); //logs onSuccess and shows what is in results
                        drinks.add(Drink.fromJsonArray(result));
                        drinkAdapter.notifyDataSetChanged();
                        Log.d(TAG, "In Drinks length: " + drinks.size());
                        //Log.i(TAG, "Drinks is: " + drinks);
                    } catch (JSONException e) {
                        Log.e(TAG, "Hit json exception", e); // handles the exception if results is not in the jsonObject
                    }
                }
                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.d(TAG, "Failure: " + response);
                }
            });

        }
        Log.d(TAG, "Out Drinks length: " + drinks.size());
    }
    public DrinkAdapter getDrinkAdapter(){
        return drinkAdapter;
    }
    public void setDrinkAdapter(DrinkAdapter DA){
        drinkAdapter = DA;
    }

    // refresh drinks in recycler view
    public void refreshDrinks(){
        drinks.clear();
        drinkAdapter.notifyDataSetChanged();
        queryDrinks(drinkAdapter, "none");
        swipeContainer.setRefreshing(false);
    }

    public void generateAds(){

        MobileAds.initialize(getContext(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });


        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(getContext(),"ca-app-pub-2454389555861292/6333761831", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

//                        Toast.makeText(MainActivity.this,"Ad Loaded", Toast.LENGTH_SHORT).show();
                        interstitialAd.show(getActivity());
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                super.onAdFailedToShowFullScreenContent(adError);
//                                Toast.makeText(MainActivity.this, "Faild to show Ad", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                super.onAdShowedFullScreenContent();
//                                Toast.makeText(MainActivity.this,"Ad Shown Successfully",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent();
                                refreshDrinks();
//                                Toast.makeText(MainActivity.this,"Ad Dismissed / Closed",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdImpression() {
                                super.onAdImpression();
//                                Toast.makeText(MainActivity.this,"Ad Impression Count",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
//                                Toast.makeText(MainActivity.this,"Ad Clicked",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        Toast.makeText(MainActivity.this,"Failed to Load Ad because="+loadAdError.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

    }
}