package cocktail.app.mixer.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.View;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import cocktail.app.mixer.DetailActivity;
import cocktail.app.mixer.Drink;
import cocktail.app.mixer.Favorites;
import cocktail.app.mixer.adapters.DrinkAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Headers;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoritesFragment extends HomeFragment {

    public static final String DRINK_URL = "https://www.thecocktaildb.com/api/json/v1/1/lookup.php?i=";
    public static final String TAG = "FavoritesFragment";    // Create a tag for logging this activity

    @Override
    public void onStart() {
        super.onStart();
        drinks.clear();
        if(DetailActivity.getKey() == 2){
            DetailActivity.setKey(0);
            getDrinkAdapter().notifyDataSetChanged();
            queryDrinks(getDrinkAdapter(), "none");        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeContainer.setRefreshing(false);
            }
        });
    }


    @Override
    protected void queryDrinks(DrinkAdapter drinkAdapter, String Placeholder) {
        ParseQuery<Favorites> query = ParseQuery.getQuery(Favorites.class);
        query.include(Favorites.KEY_USER);
        query.whereEqualTo(Favorites.KEY_USER, ParseUser.getCurrentUser());
        query.addDescendingOrder(Favorites.KEY_CREATED_AT);

        query.findInBackground(new FindCallback<Favorites>() {
            @Override
            public void done(List<Favorites> objects, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issues with getting favorites", e);
                    return;
                }

                for (Favorites favorites : objects) {
                    // Instantiate an AsyncHttpClient to execute the API request
                    AsyncHttpClient client = new AsyncHttpClient();

                    //Log.i(TAG, ", username: " + favorites.getUser().getUsername() + ", id: " + favorites.getDrinkID());
                    String tempURL = DRINK_URL + favorites.getDrinkID();

                    // Make a get request on the client object
                    client.get(tempURL , new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, JSON json) {
                            //Log.d(TAG, "Success"); // log for success, connected to TAG
                            JSONObject jsonObject = json.jsonObject; // we store the response jsonObject in the variable jsonObject
                            try {
                                JSONArray result = jsonObject.getJSONArray("drinks");
                                //Log.i(TAG, "Results: " + result.toString()); //logs onSuccess and shows what is in results
                                drinks.add(Drink.fromJsonArray(result));

                                // sort drinks alphabetically
                                Collections.sort(drinks, new Comparator<Drink>() {
                                    @Override
                                    public int compare(Drink drink, Drink t1) {
                                        return drink.getDrinkName().compareToIgnoreCase(t1.getDrinkName());
                                    }
                                });
                                drinkAdapter.notifyDataSetChanged();
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
            }
        });
    }
}