package cocktail.app.mixer.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import cocktail.app.mixer.DetailActivity;
import cocktail.app.mixer.Drink;
import cocktail.app.mixer.Favorites;
import cocktail.app.mixer.MainActivity;
import com.example.mixer.R;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.parceler.Parcels;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class DrinkAdapter extends RecyclerView.Adapter<DrinkAdapter.ViewHolder> {
    public static final String TAG = "DrinkAdapter";    // Create a tag for logging this activity

    Context context;
    List<Drink> drinks;
    DetailActivity detailActivity = new DetailActivity();


    public DrinkAdapter(Context context, List<Drink> drinks) {
        this.context = context;
        this.drinks = drinks;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        detailActivity.getFavs();
        View drinkView = LayoutInflater.from(context).inflate(R.layout.item_drink, parent, false);
        return new ViewHolder(drinkView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // Get the drink at the passed position
        Drink drink = drinks.get(position);
        Animation animation1 = AnimationUtils.loadAnimation(this.context, R.anim.fade_in);
        holder.itemView.startAnimation(animation1);
        // Bind the movie data into the VH
        holder.bind(drink);
        Log.d("Adapter", drink.toString());
    }

    @Override
    public int getItemCount() {
        return drinks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Define views
        TextView tvName;
        TextView tvAlcoholic;
        TextView tvCategory;
        ImageView ivPoster;
        RelativeLayout container;
        LikeButton icFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAlcoholic = itemView.findViewById(R.id.tvAlcoholic);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            container = itemView.findViewById(R.id.container);
            icFavorite = itemView.findViewById(R.id.icFavorite);
        }


        public void bind(Drink drink) {
            tvName.setText(drink.getDrinkName());
            tvAlcoholic.setText(drink.getDrinkIBA());
            tvCategory.setText(drink.getDrinkCategory());
            String imageUrl = drink.getPosterPath();

            // add rounded corners to images
            int radius = 30; // corner radius, higher value = more rounded
            int margin = 10; // crop margin, set to 0 for corners with no crop
            Glide.with(context).load(imageUrl).fitCenter().transform(new RoundedCornersTransformation(radius, margin)).into(ivPoster); // Binding an image is more complex, we use Glide
            queryFav(drink.getDrinkID(), icFavorite);
            icFavorite.setLiked(isFav(ParseUser.getCurrentUser(), drink.getDrinkID()));

            // Create an onClickListener on the drinks so that stuff can be done when it's clicked
            // 1. Register click listener on the whole row
            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Navigate to Detail Activity on tap
                    Log.d("Toast", "Toast clicked for drink");

                    Intent i = new Intent(context, DetailActivity.class);
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation((Activity) context, ivPoster, "activityTransition");
                    i.putExtra("drink", Parcels.wrap(drink)); // send entire drink class using Parcels to DetailActivity
                    i.putExtra("key", Parcels.wrap(MainActivity.getKey())); // send the name of the current fragment to detail activity.

                    context.startActivity(i, options.toBundle());

                }
            });
            icFavorite.setOnLikeListener(new OnLikeListener() {
                @Override
                public void liked(LikeButton likeButton) {
                    saveFav(ParseUser.getCurrentUser(), drink.getDrinkID());
                    detailActivity.getFavs();
                }

                @Override
                public void unLiked(LikeButton likeButton) {
                    Activity activity = (Activity) context;
                    unFav(drink.getDrinkID(),getAdapterPosition());
                    if(MainActivity.getKey() == 2){
                        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),"Favorite Removed..",Snackbar.LENGTH_SHORT);
                        snackbar.setAction("Undo", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                saveFav(ParseUser.getCurrentUser(),drink.getDrinkID());
                                drinks.add(drink);
                                Collections.sort(drinks, new Comparator<Drink>() {
                                    @Override
                                    public int compare(Drink drink, Drink t1) {
                                        return drink.getDrinkName().compareToIgnoreCase(t1.getDrinkName());
                                    }
                                });
                                detailActivity.getFavs();
                                notifyDataSetChanged();
                            }
                        });
                        snackbar.show();
                    }
                }
            });
        }
    }

    // This method predetermines whether the button is like or not
    private void queryFav(int drinkId, LikeButton icFavorite) {
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
//                    Log.i(TAG, "drink id = " + drinkId + "favorite Id = " + favorites.getDrinkID());
                    if (drinkId == favorites.getDrinkID()) {
                        icFavorite.setLiked(true);
                        return;
                    } else {
                        icFavorite.setLiked(false);
                    }
                }
            }
        });
    }

    private void saveFav(ParseUser currentUser, int drinkId) {
        Favorites fav = new Favorites();
        fav.setUser(currentUser);
        fav.setDrinkId(drinkId);
        fav.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issues with setting favorites", e);
                    return;
                }
            }
        });

    }

    private void unFav(int drinkId,int adapterPosition) {
        final boolean[] didUndo = {false};
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Favorites");
        query.whereEqualTo("drinkID", drinkId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                // if the error is null.
                if (e == null) {
                    // on below line we are getting the first cocktail and
                    // calling a delete method to delete this cocktail.
                    if (objects.isEmpty()){
                        notifyDataSetChanged();
                    }
                    else{
                        objects.get(0).deleteInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                // inside done method checking if the error is null or not.
                                if (e == null) {
                                    if(MainActivity.getKey() == 2){
                                        drinks.remove(adapterPosition);
                                        detailActivity.getFavs();
                                        notifyDataSetChanged();
                                    }else{
                                        Toast.makeText(context, "Favorite Removed..", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to remove Favorite..", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(context, "Failed to get the object..", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean isFav(ParseUser user, int drinkID) { // Will return true if the drink is a favorite belonging to the user.
        for (int i = 0; i < detailActivity.getFavDrinks().size(); i++) { // For all of the favorites
            if (detailActivity.getFavDrinks().get(i).getUser() == user) { // If the current favorite belongs to the user
                if (detailActivity.getFavDrinks().get(i).getDrinkID() == drinkID) { // if the drinkID of the favorite matches the input drinkID
                    return true;
                }
            }
        }
        return false;
    }
}
