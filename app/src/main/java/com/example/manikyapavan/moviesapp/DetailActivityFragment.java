package com.example.manikyapavan.moviesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;



import com.bumptech.glide.Glide;
import com.example.manikyapavan.moviesapp.Config.Config;
import com.example.manikyapavan.moviesapp.adapters.ReviewAdapter;
import com.example.manikyapavan.moviesapp.adapters.TrailerAdapter;
import com.example.manikyapavan.moviesapp.database.MovieContract;
import com.example.manikyapavan.moviesapp.modal.Movie;
import com.example.manikyapavan.moviesapp.modal.Review;
import com.example.manikyapavan.moviesapp.modal.Trailer;
import com.linearlistview.LinearListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DetailActivityFragment extends Fragment {

    public static final String TAG = DetailActivityFragment.class.getSimpleName();

    static final String DETAIL_MOVIE = "DETAIL_MOVIE";

    private Movie mMovie;

    private ImageView mImageView;

    private TextView mTitleView;
    private TextView mOverviewView;
    private TextView mDateView;
    private TextView mVoteAverageView;

    private LinearListView mTrailersView;
    private LinearListView mReviewsView;

    private CardView mReviewsCardview;
    private CardView mTrailersCardview;

    private TrailerAdapter mTrailerAdapter;
    private ReviewAdapter mReviewAdapter;

    private ScrollView mDetailLayout;

    private Toast mToast;

    private ShareActionProvider mShareActionProvider;

    // the first trailer video to share
    private Trailer mTrailer;

    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mMovie != null) {
            inflater.inflate(R.menu.menu_fragment_detail, menu);

            final MenuItem action_favorite = menu.findItem(R.id.action_favorite);
            MenuItem action_share = menu.findItem(R.id.action_share);

            new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    return Utility.isFavorited(getActivity(), mMovie.getId());
                }

                @Override
                protected void onPostExecute(Integer isFavorited) {
                    action_favorite.setIcon(isFavorited == 1 ?
                            R.drawable.favorite_added :
                            R.drawable.favorite_removed);
                }
            }.execute();

            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(action_share);

            if (mTrailer != null) {
                mShareActionProvider.setShareIntent(createShareMovieIntent());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_favorite:
                if (mMovie != null) {
                    // check if movie is in favorites or not
                    new AsyncTask<Void, Void, Integer>() {

                        @Override
                        protected Integer doInBackground(Void... params) {
                            return Utility.isFavorited(getActivity(), mMovie.getId());
                        }

                        @Override
                        protected void onPostExecute(Integer isFavorited) {
                            // if it is in favorites
                            if (isFavorited == 1) {
                                // delete from favorites
                                new AsyncTask<Void, Void, Integer>() {
                                    @Override
                                    protected Integer doInBackground(Void... params) {
                                        return getActivity().getContentResolver().delete(
                                                MovieContract.MovieEntry.CONTENT_URI,
                                                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                                                new String[]{Integer.toString(mMovie.getId())}
                                        );
                                    }

                                    @Override
                                    protected void onPostExecute(Integer rowsDeleted) {
                                        item.setIcon(R.drawable.favorite_removed);
                                        if (mToast != null) {
                                            mToast.cancel();
                                        }
                                        mToast = Toast.makeText(getActivity(), getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT);
                                        mToast.show();
                                    }
                                }.execute();
                            }
                            // if it is not in favorites
                            else {
                                // add to favorites
                                new AsyncTask<Void, Void, Uri>() {
                                    @Override
                                    protected Uri doInBackground(Void... params) {
                                        ContentValues values = new ContentValues();

                                        values.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, mMovie.getId());
                                        values.put(MovieContract.MovieEntry.COLUMN_TITLE, mMovie.getTitle());
                                        values.put(MovieContract.MovieEntry.COLUMN_IMAGE, mMovie.getImage());
                                        values.put(MovieContract.MovieEntry.COLUMN_IMAGE2, mMovie.getImage2());
                                        values.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, mMovie.getOverview());
                                        values.put(MovieContract.MovieEntry.COLUMN_RATING, mMovie.getRating());
                                        values.put(MovieContract.MovieEntry.COLUMN_DATE, mMovie.getDate());

                                        return getActivity().getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI,
                                                values);
                                    }

                                    @Override
                                    protected void onPostExecute(Uri returnUri) {
                                        item.setIcon(R.drawable.favorite_added);
                                        if (mToast != null) {
                                            mToast.cancel();
                                        }
                                        mToast = Toast.makeText(getActivity(), getString(R.string.added_to_favorites), Toast.LENGTH_SHORT);
                                        mToast.show();
                                    }
                                }.execute();
                            }
                        }
                    }.execute();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mMovie = arguments.getParcelable(DetailActivityFragment.DETAIL_MOVIE);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mDetailLayout = (ScrollView) rootView.findViewById(R.id.detail_layout);

        if (mMovie != null) {
            mDetailLayout.setVisibility(View.VISIBLE);
        } else {
            mDetailLayout.setVisibility(View.INVISIBLE);
        }

        mImageView = (ImageView) rootView.findViewById(R.id.detail_image);

        mTitleView = (TextView) rootView.findViewById(R.id.detail_title);
        mOverviewView = (TextView) rootView.findViewById(R.id.detail_overview);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date);
        mVoteAverageView = (TextView) rootView.findViewById(R.id.detail_vote_average);

        mTrailersView = (LinearListView) rootView.findViewById(R.id.detail_trailers);
        mReviewsView = (LinearListView) rootView.findViewById(R.id.detail_reviews);

        mReviewsCardview = (CardView) rootView.findViewById(R.id.detail_reviews_cardview);
        mTrailersCardview = (CardView) rootView.findViewById(R.id.detail_trailers_cardview);

        mTrailerAdapter = new TrailerAdapter(getActivity(), new ArrayList<Trailer>());
        mTrailersView.setAdapter(mTrailerAdapter);

        mTrailersView.setOnItemClickListener(new LinearListView.OnItemClickListener() {
            @Override
            public void onItemClick(LinearListView linearListView, View view,
                                    int position, long id) {
                Trailer trailer = mTrailerAdapter.getItem(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.youtube.com/watch?v=" + trailer.getKey()));
                startActivity(intent);
            }
        });

        mReviewAdapter = new ReviewAdapter(getActivity(), new ArrayList<Review>());
        mReviewsView.setAdapter(mReviewAdapter);

        if (mMovie != null) {

            String image_url = Utility.buildImageUrl(342, mMovie.getImage2());

            Glide.with(this).load(image_url).into(mImageView);

            mTitleView.setText(mMovie.getTitle());
            mOverviewView.setText(mMovie.getOverview());

            String movie_date = mMovie.getDate();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                String date = DateUtils.formatDateTime(getActivity(),
                        formatter.parse(movie_date).getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
                mDateView.setText(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            mVoteAverageView.setText(Integer.toString(mMovie.getRating()) + "/10");
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMovie != null) {
            new FetchTrailersTask().execute(Integer.toString(mMovie.getId()));
            new FetchReviewsTask().execute(Integer.toString(mMovie.getId()));
        }
    }

    private Intent createShareMovieIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mMovie.getTitle() + " " +
                "http://www.youtube.com/watch?v=" + mTrailer.getKey());
        return shareIntent;
    }

    public class FetchTrailersTask extends AsyncTask<String, Void, List<Trailer>> {

        private final String LOG_TAG = FetchTrailersTask.class.getSimpleName();

        private List<Trailer> getTrailersDataFromJson(String jsonStr) throws JSONException {
            JSONObject trailerJson = new JSONObject(jsonStr);
            JSONArray trailerArray = trailerJson.getJSONArray("results");

            List<Trailer> results = new ArrayList<>();

            for(int i = 0; i < trailerArray.length(); i++) {
                JSONObject trailer = trailerArray.getJSONObject(i);
                // Only show Trailers which are on Youtube
                if (trailer.getString("site").contentEquals("YouTube")) {
                    Trailer trailerModel = new Trailer(trailer);
                    results.add(trailerModel);
                }
            }

            return results;
        }

        @Override
        protected List<Trailer> doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonStr = null;

            try {
                final String BASE_URL = "http://api.themoviedb.org/3/movie/" + params[0] + "/videos";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, Config.API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getTrailersDataFromJson(jsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(List<Trailer> trailers) {
            if (trailers != null) {
                if (trailers.size() > 0) {
                    mTrailersCardview.setVisibility(View.VISIBLE);
                    if (mTrailerAdapter != null) {
                        mTrailerAdapter.clear();
                        for (Trailer trailer : trailers) {
                            mTrailerAdapter.add(trailer);
                        }
                    }

                    mTrailer = trailers.get(0);
                    if (mShareActionProvider != null) {
                        mShareActionProvider.setShareIntent(createShareMovieIntent());
                    }
                }
            }
        }
    }

    public class FetchReviewsTask extends AsyncTask<String, Void, List<Review>> {

        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();

        private List<Review> getReviewsDataFromJson(String jsonStr) throws JSONException {
            JSONObject reviewJson = new JSONObject(jsonStr);
            JSONArray reviewArray = reviewJson.getJSONArray("results");

            List<Review> results = new ArrayList<>();

            for(int i = 0; i < reviewArray.length(); i++) {
                JSONObject review = reviewArray.getJSONObject(i);
                results.add(new Review(review));
            }

            return results;
        }

        @Override
        protected List<Review> doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonStr = null;

            try {
                final String BASE_URL = "http://api.themoviedb.org/3/movie/" + params[0] + "/reviews";
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, Config.API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                jsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getReviewsDataFromJson(jsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(List<Review> reviews) {
            if (reviews != null) {
                if (reviews.size() > 0) {
                    mReviewsCardview.setVisibility(View.VISIBLE);
                    if (mReviewAdapter != null) {
                        reviews.clear();
                        for (Review review : reviews) {
                            reviews.add(review);
                        }
                    }
                }
            }
        }
    }
}
