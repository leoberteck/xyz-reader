package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.adapter.ArticleBodyAdapter;
import com.example.xyzreader.data.ArticleLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private ImageView mPhotoView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private DateFormat outputFormat = DateFormat.getDateInstance();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    private boolean isLoading = false;

    public ArticleDetailActivity getDetailActivity(){
        return (ArticleDetailActivity) getActivity();
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain")
                    .setText("Some sample text")
                    .getIntent(), getString(R.string.action_share)));
            }
        });
        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void bindViews() {
        if(!isLoading){
            isLoading = true;
            new AsyncTask<Object, Void, Void>() {
                String articleTitle = null;
                String articleBy = null;
                byte[] articleBody = null;
                String photoUrl = null;
                @Override
                protected Void doInBackground(Object... params) {
                    if (mCursor != null) {
                        if(TextUtils.isEmpty(articleTitle)){
                            articleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
                        }
                        if(TextUtils.isEmpty(articleBy)){
                            Date publishedDate = parsePublishedDate();
                            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                                articleBy = DateUtils.getRelativeTimeSpanString(
                                        publishedDate.getTime(),
                                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                        DateUtils.FORMAT_ABBREV_ALL).toString()
                                        + " by <font color='#ffffff'>"
                                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                        + "</font>";

                            } else {
                                // If date is before 1902, just show the string
                                articleBy = outputFormat.format(publishedDate)
                                        + " by <font color='#ffffff'>"
                                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                        + "</font>";
                            }
                        }
                        if(articleBody == null){
                            //articleBody = mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />");
                            articleBody = mCursor.getBlob(ArticleLoader.Query.BODY);
                        }
                        if(TextUtils.isEmpty(photoUrl)){
                            photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    bindViews(articleTitle, articleBy, articleBody, photoUrl);
                    isLoading = false;
                }
            }.execute();
        }
    }

    private void bindViews(String articleTitle, String articleBy, byte[] articleBody, String photoUrl) {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        RecyclerView bodyView = (RecyclerView) mRootView.findViewById(R.id.article_body);

        mRootView.setAlpha(0);
        mRootView.setVisibility(View.VISIBLE);
        mRootView.animate().alpha(1);
        titleView.setText(articleTitle);
        if(!TextUtils.isEmpty(articleBy)){
            bylineView.setText(Html.fromHtml(articleBy));
        }
        if(articleBody != null){
            bodyView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
            bodyView.setAdapter(new ArticleBodyAdapter(articleBody));
        }
        if(!TextUtils.isEmpty(photoUrl)){
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                .get(photoUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            mPhotoView.setImageBitmap(imageContainer.getBitmap());
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
