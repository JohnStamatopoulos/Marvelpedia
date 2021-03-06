package com.project_future_2021.marvelpedia.recycler_view;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project_future_2021.marvelpedia.R;
import com.project_future_2021.marvelpedia.data.Hero;

import java.util.List;

public class MyListAdapter
        extends ListAdapter<Hero, MyListAdapter.MyViewHolder> {

    private static final String TAG = "MyListAdapter";
    private final List<Hero> myAdapterHeroesList;
    private final myClickListener myClickListener;
    private int lastPosition = -1;

    public MyListAdapter(@NonNull DiffUtil.ItemCallback<Hero> diffCallback, List<Hero> myAdapterHeroesList, myClickListener myClickListener) {
        super(diffCallback);
        this.myAdapterHeroesList = myAdapterHeroesList;
        this.myClickListener = myClickListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_list_item, parent, false);
        return new MyViewHolder(view, myClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bind(getItem(position), position);

        // TODO: do we want or not to have animation on the RecyclerView?
        //holder.setAnimation(holder.itemView, position);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MyViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        // Stop animating items the user can't see.
        holder.clearAnimation();
    }

    public interface myClickListener {
        void onClick(View v, Hero data);

        void onFavoritePressed(View v, Hero data, int position);
    }

    // Get the Hero at this position, used in FavoritesFragment
    // by our ItemTouchHelper (swipe items left/right).
    public Hero getHeroAt(int position) {
        return getItem(position);
    }

    public static class HeroDiff extends DiffUtil.ItemCallback<Hero> {
        @Override
        public boolean areItemsTheSame(@NonNull Hero oldItem,
                                       @NonNull Hero newItem) {
            return oldItem.getId().equals(newItem.getId());
            //return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Hero oldItem,
                                          @NonNull Hero newItem) {
            return oldItem.equals(newItem);
            //return oldItem.equalsContent(newItem);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final myClickListener myClickListener;

        public MyViewHolder(@NonNull View itemView, myClickListener myClickListener) {
            super(itemView);
            this.myClickListener = myClickListener;
        }

        /**
         * Here is the key method to apply the animation
         */
        private void setAnimation(View viewToAnimate, int position) {
            // If the bound view wasn't previously displayed on screen, it's animated
            if (position > lastPosition) {
                //Animation animation = AnimationUtils.loadAnimation(itemView.getContext(), android.R.anim.slide_in_left);
                Animation animation = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.recycler_view_fall_down);
                viewToAnimate.startAnimation(animation);

                lastPosition = position;
            }
        }

        public void clearAnimation() {
            itemView.getRootView().clearAnimation();
        }

        public void bind(Hero data, int position) {
            TextView hero_name = itemView.findViewById(R.id.sharedTextViewHeroName);
            TextView hero_description = itemView.findViewById(R.id.sharedTextViewHeroDescription);
            ImageView hero_thumbnail = itemView.findViewById(R.id.sharedImageViewHeroThumbnail);

            // SOS#1: "ViewCompat.setTransitionName(View v, String s)" is super-duper important to add, it gives unique names to our intended-to-be shared views.
            // If we do not add them, we get a "java.lang.IllegalArgumentException: Unique transitionNames are required for all sharedElements" exception.
            // SOS#2: Each view must have unique names, so here, we take the hero's id(which is unique to each hero)
            // AND
            // we then add "thumbnail", or "description123", or ...anything, to have a unique String as transitionName in the end.
            // If we do not make them unique, we get this error: "A shared element with the source name '1017100' has already been added to the transaction."
            ViewCompat.setTransitionName(hero_name, "name" + data.getId().toString());
            ViewCompat.setTransitionName(hero_description, "description" + data.getId().toString());
            ViewCompat.setTransitionName(hero_thumbnail, "thumbnail" + data.getId().toString());

            ImageView hero_favorite = itemView.findViewById(R.id.holder_list_hero_favorite);
            if (data.getFavorite()) {
                hero_favorite.setImageResource(R.drawable.ic_yes_favorite);
            } else {
                hero_favorite.setImageResource(R.drawable.ic_no_favorite);
            }

            hero_name.setText(data.getName());
            hero_description.setText(data.getDescription());

            //if (data.getThumbnail()!=null){
            Glide.with(itemView)
                    .load(data.getThumbnail().makeImageWithVariant("portrait_xlarge"))
                    .placeholder(R.drawable.ic_image_placeholder2)
                    .into(hero_thumbnail);
            //}
            /*else{
                Glide.with(itemView)
                        .load(R.drawable.ic_baseline_image_search_24)
                        .into(hero_thumbnail);
            }*/

            // Set up 2 click listeners for each item.
            // 1st case is when users click on 'favorites' icon
            // 2nd case is when users click anywhere else.

            // 1/2 click events
            hero_favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position != RecyclerView.NO_POSITION) {
                        myClickListener.onFavoritePressed(v, data, position);
                        // TODO here or in the Fragment?
                        notifyItemChanged(position);
                    } else {
                        Log.d(TAG, "favorite-onClick: something bad was about to happen");
                    }
                }
            });
            // 2/2 click events
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (position != RecyclerView.NO_POSITION) {
                        myClickListener.onClick(v, data);
                    } else {
                        Log.d(TAG, "simple-onClick: something bad was about to happen");
                    }
                }
            });

        }
    }
}