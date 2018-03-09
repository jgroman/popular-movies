/*
 * Copyright 2018 Jaroslav Groman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.jtek.popularmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.MovieGridAdapterViewHolder> {

    public interface MovieGridOnClickHandler {
        void onClick(int itemId);
    }

    private List<TmdbData.Movie> mMovieList;
    final private MovieGridOnClickHandler mClickHandler;


    /**
     * Class constructor - creates MovieGridAdapter.
     *
     * @param clickHandler OnClick handler for this adapter. It is called when grid item is clicked.
     *
     */
    public MovieGridAdapter(MovieGridOnClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public class MovieGridAdapterViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public final TextView mMovieTextView;

        // Attach OnClick listener when creating view
        public MovieGridAdapterViewHolder(View view) {
            super(view);
            mMovieTextView = (TextView) view.findViewById(R.id.tv_movie_item);
            view.setOnClickListener(this);
        }

        /**
         * This gets called by the child views during a click.
         *
         * @param view The View that was clicked
         */
        @Override
        public void onClick(View view) {
            int itemPos = getAdapterPosition();
            mClickHandler.onClick(itemPos);
        }
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param parent   The ViewGroup that these ViewHolders are contained within.
     * @param viewType If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new MovieGridAdapterViewHolder that holds the View for each list item
     */
    @Override
    public MovieGridAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int gridItemLayoutId = R.layout.movie_recycler_item;

        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(gridItemLayoutId, parent, shouldAttachToParentImmediately);
        return new MovieGridAdapterViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the movie
     * data for this particular position addressed by "position" parameter.
     *
     * @param holder    The ViewHolder which should be updated to represent the
     *                   contents of the item at the given position in the data set.
     * @param position  The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(MovieGridAdapterViewHolder holder, int position) {
        String itemText = mMovieList.get(position).getTitle();
        holder.mMovieTextView.setText(itemText);
    }

    /**
     * This method returns the number of items to display.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (null == mMovieList) return 0;
        return mMovieList.size();
    }

    /**
     * This method is used to set the movie data on a MovieGridAdapter if we've already
     * created one.
     *
     * @param tmdbData The new movie data to be displayed.
     */
    public void setMovieData(TmdbData tmdbData) {
        mMovieList = tmdbData.getMovieList();
        notifyDataSetChanged();
    }
}