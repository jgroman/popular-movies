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

    private List<TmdbApi.Movie> mMovieList;
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

    public class MovieGridAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mMovieTextView;

        // Attach OnClick listener when creating view
        public MovieGridAdapterViewHolder(View view) {
            super(view);
            mMovieTextView = (TextView) view.findViewById(R.id.tv_movie_item);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int itemPos = getAdapterPosition();
            mClickHandler.onClick(itemPos);
        }
    }

    @Override
    public MovieGridAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int gridItemLayoutId = R.layout.movie_recycler_item;

        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(gridItemLayoutId, parent, shouldAttachToParentImmediately);
        return new MovieGridAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MovieGridAdapterViewHolder holder, int position) {
        String itemText = mMovieList.get(position).getTitle();
        holder.mMovieTextView.setText(itemText);
    }

    @Override
    public int getItemCount() {
        if (null == mMovieList) return 0;
        return mMovieList.size();
    }
}
