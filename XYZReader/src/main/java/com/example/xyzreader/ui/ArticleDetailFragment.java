package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.button.MaterialButton;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_STARTING_ARTICLE_IMAGE_POSITION = "arg_starting_article_position";
    private static final String ARG_ARTICLE_IMAGE_POSITION = "arg_article_position";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private String transitionName;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private String[] text;

    // View Binding
    @BindView(R.id.photo) ThreeTwoImageView mPhotoView;
    @BindView(R.id.share_fab) FloatingActionButton shareFab;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.collapsingToolbar) CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.article_title) TextView titleView;
    @BindView(R.id.article_byline) TextView bylineView;
    @BindView(R.id.article_body) TextView bodyView;
    @BindView(R.id.article_body_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.button_more) MaterialButton readMore;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private int articlePosition;
    private int startingPosition;
    private boolean isTransitioning;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_ARTICLE_IMAGE_POSITION, position);
        arguments.putInt(ARG_STARTING_ARTICLE_IMAGE_POSITION, position);

        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startingPosition = getArguments().getInt(ARG_STARTING_ARTICLE_IMAGE_POSITION);
        articlePosition = getArguments().getInt(ARG_ARTICLE_IMAGE_POSITION);
        isTransitioning = savedInstanceState == null && startingPosition == articlePosition;

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    private void setUpThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String wholeText = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                        .replaceAll("(\r\n|\n)", "<br />")).toString();
                final String[] splitContent = wholeText.split("<br />");

                if (splitContent.length > 0){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setUpRecyclerView(splitContent);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        transitionName = String.valueOf(articlePosition);
        mPhotoView.setTransitionName(transitionName);

        shareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });


        toolbar.setTitle("");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finishAfterTransition();
            }
        });

        collapsingToolbarLayout.setTitleEnabled(true);

        readMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bodyView.setVisibility(View.GONE);
                readMore.setVisibility(View.GONE);

                setUpThread();
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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        bylineView.setMovementMethod(new LinkMovementMethod());

        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            collapsingToolbarLayout.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .substring(0, 200)
                    .replaceAll("(\r\n|\n)", "<br />")));

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            //scheduleStartPostponedTransition(mPhotoView);
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                try {
                                    Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(@Nullable Palette palette) {
                                            if (palette != null) {
                                                HashMap map = processPalette(palette);

                                                int statusBarScrimColor;

                                                if(map.containsKey("Muted") && map.containsKey("DarkMuted")) {

                                                    mMutedColor = ((Palette.Swatch) map.get("Muted")).getRgb();
                                                    statusBarScrimColor = ((Palette.Swatch) map.get("DarkMuted")).getRgb();

                                                    collapsingToolbarLayout.setBackgroundColor(mMutedColor);
                                                    collapsingToolbarLayout.setContentScrimColor(mMutedColor);
                                                    getActivity().getWindow().setStatusBarColor(statusBarScrimColor);

                                                } else if(map.containsKey("Vibrant") && map.containsKey("DarkVibrant")) {

                                                    mMutedColor = ((Palette.Swatch) map.get("Vibrant")).getRgb();
                                                    statusBarScrimColor = ((Palette.Swatch) map.get("DarkVibrant")).getRgb();

                                                    collapsingToolbarLayout.setBackgroundColor(mMutedColor);
                                                    collapsingToolbarLayout.setContentScrimColor(mMutedColor);
                                                    getActivity().getWindow().setStatusBarColor(statusBarScrimColor);
                                                }
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            //scheduleStartPostponedTransition(mPhotoView);
                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    private void setUpRecyclerView(String[] text) {
        ArticleBodyAdapter adapter = new ArticleBodyAdapter(text);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL,
                false);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        customStartPostponedEnterTransition();

        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    private void customStartPostponedEnterTransition(){
        if (articlePosition == startingPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    try {
                        getActivity().startPostponedEnterTransition();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private HashMap<String, Palette.Swatch> processPalette(Palette palette){

        HashMap<String, Palette.Swatch> returnHashMap = new HashMap<>();

        if (palette.getVibrantSwatch() != null)
            returnHashMap.put("Vibrant", palette.getVibrantSwatch());
        if (palette.getDarkVibrantSwatch() != null)
            returnHashMap.put("DarkVibrant", palette.getDarkVibrantSwatch());
        if (palette.getLightVibrantSwatch() != null)
            returnHashMap.put("LightVibrant", palette.getLightVibrantSwatch());

        if (palette.getMutedSwatch() != null)
            returnHashMap.put("Muted", palette.getMutedSwatch());
        if (palette.getDarkMutedSwatch() != null)
            returnHashMap.put("DarkMuted", palette.getDarkMutedSwatch());
        if (palette.getLightMutedSwatch() != null)
            returnHashMap.put("LightMuted", palette.getLightMutedSwatch());

        return returnHashMap;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view){
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    @Nullable
    public ThreeTwoImageView getArticlePhotoView(){
        boolean isViewInBounds = false;
        try {
            isViewInBounds = isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isViewInBounds)
            return mPhotoView;

        return null;
    }

    public class ArticleBodyAdapter extends RecyclerView.Adapter<ArticleBodyAdapter.ViewHolder>{

        private String[] body;

        private ArticleBodyAdapter(String[] body){
            this.body = body;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View view = inflater.inflate(R.layout.article_body_item, viewGroup, false);
            ViewHolder vh = new ViewHolder(view);
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            viewHolder.bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
            viewHolder.bodyView.setText(body[position]);
        }

        @Override
        public int getItemCount() {
            if (body != null && body.length != 0)
                return body.length;
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder{

            @BindView(R.id.article_body) TextView bodyView;

            private ViewHolder(@NonNull View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
