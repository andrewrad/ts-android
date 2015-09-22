package com.door43.translationstudio.newui.translate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Chapter;
import com.door43.translationstudio.core.ChapterTranslation;
import com.door43.translationstudio.core.Frame;
import com.door43.translationstudio.core.FrameTranslation;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.TranslationNote;
import com.door43.translationstudio.core.SourceLanguage;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetLanguage;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.TranslationFormat;
import com.door43.translationstudio.core.TranslationWord;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.core.Typography;
import com.door43.translationstudio.rendering.DefaultRenderer;
import com.door43.translationstudio.rendering.RenderingGroup;
import com.door43.translationstudio.rendering.USXRenderer;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by joel on 9/18/2015.
 */
public class ReviewModeAdapter extends ViewModeAdapter<ReviewModeAdapter.ViewHolder> {

    private final Library mLibrary;
    private final Translator mTranslator;
    private final Activity mContext;
    private final TargetTranslation mTargetTranslation;
    private SourceTranslation mSourceTranslation;
    private SourceLanguage mSourceLanguage;
    private final TargetLanguage mTargetLanguage;
    private Frame[] mFrames;
    private CharSequence[] mRenderedSourceBody;
    private CharSequence[] mRenderedTargetBody;
    private int mLayoutBuildNumber = 0;
    private boolean mResourcesOpened = false;

    public ReviewModeAdapter(Activity context, String targetTranslationId, String sourceTranslationId, String chapterId, String frameId) {
        mLibrary = AppContext.getLibrary();
        mTranslator = AppContext.getTranslator();
        mContext = context;
        mTargetTranslation = mTranslator.getTargetTranslation(targetTranslationId);
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);
        mTargetLanguage = mLibrary.getTargetLanguage(mTargetTranslation.getTargetLanguageId());

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            if(chapterId != null && c.getId().equals(chapterId) && chapterFrames.length > 0) {
                // identify starting selection
                setListStartPosition(frames.size());
                if(frameId != null) {
                    for(Frame frame:chapterFrames) {
                        if(frame.getId().equals(frameId)) {
                            setListStartPosition(frames.size());
                        }
                        frames.add(frame);
                    }
                    continue;
                }
            }
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];
    }

    @Override
    void rebuild() {
        mLayoutBuildNumber ++;
        notifyDataSetChanged();
    }

    @Override
    void setSourceTranslation(String sourceTranslationId) {
        mSourceTranslation = mLibrary.getSourceTranslation(sourceTranslationId);
        mSourceLanguage = mLibrary.getSourceLanguage(mSourceTranslation.projectId, mSourceTranslation.sourceLanguageId);

        Chapter[] chapters = mLibrary.getChapters(mSourceTranslation);
        List<Frame> frames = new ArrayList<>();
        for(Chapter c:chapters) {
            Frame[] chapterFrames = mLibrary.getFrames(mSourceTranslation, c.getId());
            frames.addAll(Arrays.asList(chapterFrames));
        }
        mFrames = frames.toArray(new Frame[frames.size()]);
        mRenderedSourceBody = new CharSequence[mFrames.length];
        mRenderedTargetBody = new CharSequence[mFrames.length];

        notifyDataSetChanged();
    }

    @Override
    void onCoordinate(final ViewHolder holder) {
        int durration = 400;
        float openWeight = 1f;
        float closedWeight = 0.765f;
        ObjectAnimator anim;
        if(mResourcesOpened) {
            anim = ObjectAnimator.ofFloat(holder.mMainContent, "weightSum", openWeight, closedWeight);
        } else {
            anim = ObjectAnimator.ofFloat(holder.mMainContent, "weightSum", closedWeight, openWeight);
        }
        anim.setDuration(durration);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                holder.mMainContent.requestLayout();
            }
        });
        anim.start();
    }

    @Override
    public String getFocusedFrameId(int position) {
        if(position >= 0 && position < mFrames.length) {
            return mFrames[position].getId();
        }
        return null;
    }

    @Override
    public String getFocusedChapterId(int position) {
        if(position >= 0 && position < mFrames.length) {
            return mFrames[position].getChapterId();
        }
        return null;
    }

    @Override
    public ViewHolder onCreateManagedViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_review_list_item, parent, false);
        ViewHolder vh = new ViewHolder(parent.getContext(), v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Frame frame = mFrames[position];

        // render the source frame body
        if(mRenderedSourceBody[position] == null) {
            mRenderedSourceBody[position] = renderText(frame.body, frame.getFormat());
        }

        holder.mSourceBody.setText(mRenderedSourceBody[position]);

        // render source frame title (we don't actually set the source title)
        Chapter chapter = mLibrary.getChapter(mSourceTranslation, frame.getChapterId());
        String sourceChapterTitle = chapter.title;
        if(chapter.title.isEmpty()) {
            sourceChapterTitle = mSourceTranslation.getProjectTitle() + " " + Integer.parseInt(chapter.getId());
        }
        sourceChapterTitle += ":" + frame.getTitle();

        // render the target frame body
        if(mRenderedTargetBody[position] == null) {
            FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
            mRenderedTargetBody[position] = renderText(frameTranslation.body, frameTranslation.getFormat());
        }
        if(holder.mTextWatcher != null) {
            holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
        }
        holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));

        // render target frame title
        ChapterTranslation chapterTranslation = mTargetTranslation.getChapterTranslation(chapter);
        String targetChapterTitle = chapterTranslation.title;
        final FrameTranslation frameTranslation = mTargetTranslation.getFrameTranslation(frame);
        if(!targetChapterTitle.isEmpty()) {
            targetChapterTitle += ":" + frameTranslation.getTitle();
        } else {
            targetChapterTitle = sourceChapterTitle;
        }
        holder.mTargetTitle.setText(targetChapterTitle + " - " + mTargetLanguage.name);

        // load tabs
        holder.mTabLayout.setOnTabSelectedListener(null);
        holder.mTabLayout.removeAllTabs();
        String[] sourceTranslationIds = mTranslator.getSourceTranslations(mTargetTranslation.getId());
        for(String id:sourceTranslationIds) {
            SourceTranslation sourceTranslation = mLibrary.getSourceTranslation(id);
            if(sourceTranslation != null) {
                TabLayout.Tab tab = holder.mTabLayout.newTab();
                // include the resource id if there are more than one
                if(mLibrary.getResources(sourceTranslation.projectId, sourceTranslation.sourceLanguageId).length > 1) {
                    tab.setText(sourceTranslation.getSourceLanguageTitle() + " " + sourceTranslation.resourceId.toUpperCase());
                } else {
                    tab.setText(sourceTranslation.getSourceLanguageTitle());
                }
                tab.setTag(sourceTranslation.getId());
                holder.mTabLayout.addTab(tab);
            }
        }

        // select correct tab
        for(int i = 0; i < holder.mTabLayout.getTabCount(); i ++) {
            TabLayout.Tab tab = holder.mTabLayout.getTabAt(i);
            if(tab.getTag().equals(mSourceTranslation.getId())) {
                tab.select();
                break;
            }
        }

        // hook up listener
        holder.mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final String sourceTranslationId = (String) tab.getTag();
                if (getListener() != null) {
                    Handler hand = new Handler(Looper.getMainLooper());
                    hand.post(new Runnable() {
                        @Override
                        public void run() {
                            getListener().onTabClick(sourceTranslationId);
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        holder.mNewTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onNewTabClick();
                }
            }
        });

        holder.mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: handle the edit button click
            }
        });

        holder.mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // save
//                TODO: do this so we don't have to wait for compiling
//                Translator.applyFrameTranslation(frameTranslation, (Editable)s);

                String translation = Translator.compileTranslation((Editable)s);
                mTargetTranslation.applyFrameTranslation(frameTranslation, translation);

                // TODO: we either need to force the translation to save when the view leaves the window (so we have it if they come back before it was saved)
                // or just always save immediately

                mRenderedTargetBody[position] = renderText(translation, frameTranslation.getFormat());

                // update view
                // TRICKY: anything worth updating will need to change by at least 7 characters
                // <a></a> <-- at least 7 characters are required to create a tag for rendering.
                int minDeviation = 7;
                if(count - before > minDeviation) {
                    int scrollX = holder.mTargetBody.getScrollX();
                    int scrollY = holder.mTargetBody.getScrollX();
                    int selection = holder.mTargetBody.getSelectionStart();

                    holder.mTargetBody.removeTextChangedListener(holder.mTextWatcher);
                    holder.mTargetBody.setText(TextUtils.concat(mRenderedTargetBody[position], "\n"));
                    holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

                    holder.mTargetBody.scrollTo(scrollX, scrollY);
                    if(selection > holder.mTargetBody.length()) {
                        selection = holder.mTargetBody.length();
                    }
                    holder.mTargetBody.setSelection(selection);
                    holder.mTargetBody.clearFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        holder.mTargetBody.addTextChangedListener(holder.mTextWatcher);

        // set up fonts
        if(holder.mLayoutBuildNumber != mLayoutBuildNumber) {
            holder.mLayoutBuildNumber = mLayoutBuildNumber;
            Typography.format(mContext, holder.mSourceBody, mSourceLanguage.getId(), mSourceLanguage.getDirection());
            Typography.formatSub(mContext, holder.mTargetTitle, mTargetLanguage.getId(), mTargetLanguage.getDirection());
            Typography.format(mContext, holder.mTargetBody, mTargetLanguage.getId(), mTargetLanguage.getDirection());
        }

        if(mResourcesOpened) {
            holder.mMainContent.setWeightSum(.765f);
        } else {
            holder.mMainContent.setWeightSum(1f);
        }

        // resources tabs
        TranslationNote[] notes = mLibrary.getTranslationNotes(mSourceTranslation, frame.getChapterId(), frame.getId());
        if(notes.length > 0) {
            // TODO: add notes tab
        } else {
            // TODO: remove notes tab
        }

        TranslationWord[] words = mLibrary.getTranslationWords(mSourceTranslation, frame.getChapterId(), frame.getId());
        if(words.length > 0) {
            // TODO: add words tab
        } else {
            // TODO: remove words tab
        }

        if(notes.length == 0 && words.length == 0) {
            holder.mResourceLayout.setVisibility(View.GONE);
        } else {
            holder.mResourceLayout.setVisibility(View.VISIBLE);
        }

        // done buttons
        holder.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: display confirmation dialog
                if(mTargetTranslation.finishFrame(frame)) {
                    holder.mDoneButton.setVisibility(View.GONE);
                    holder.mDoneFlag.setVisibility(View.VISIBLE);
                } else {
                    Snackbar snack = Snackbar.make(mContext.findViewById(android.R.id.content), R.string.translate_first, Snackbar.LENGTH_LONG);
                    ViewUtil.setSnackBarTextColor(snack, mContext.getResources().getColor(R.color.light_primary_text));
                    snack.show();
                }
            }
        });
        holder.mDoneFlag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTargetTranslation.reopenFrame(frame)) {
                    // TODO: display confirmation dialog
                    holder.mDoneButton.setVisibility(View.VISIBLE);
                    holder.mDoneFlag.setVisibility(View.GONE);
                }
            }
        });
        if(frameTranslation.isFinished()) {
            holder.mDoneButton.setVisibility(View.GONE);
            holder.mDoneFlag.setVisibility(View.VISIBLE);
        } else {
            holder.mDoneButton.setVisibility(View.VISIBLE);
            holder.mDoneFlag.setVisibility(View.GONE);
        }
    }

    private CharSequence renderText(String text, TranslationFormat format) {
        RenderingGroup renderingGroup = new RenderingGroup();
        if (format == TranslationFormat.USX) {
            // TODO: add click listeners for verses and notes
            renderingGroup.addEngine(new USXRenderer(null, null));
        } else {
            // TODO: add note click listener
            renderingGroup.addEngine(new DefaultRenderer(null));
        }
        renderingGroup.init(text);
        return renderingGroup.start();
    }

    @Override
    public int getItemCount() {
        return mFrames.length;
    }

    /**
     * opens the resources view
     */
    public void openResources() {
        if(!mResourcesOpened) {
            mResourcesOpened = true;
            coordinateViewHolders();
        }
    }

    /**
     * closes the resources view
     */
    public void closeResources() {
        if(mResourcesOpened) {
            mResourcesOpened = false;
            coordinateViewHolders();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageButton mEditButton;
        public final CardView mResourceCard;
        public final LinearLayout mMainContent;
        public final LinearLayout mResourceLayout;
        public final Button mDoneButton;
        private final LinearLayout mDoneFlag;
        public int mLayoutBuildNumber = -1;
        public TextWatcher mTextWatcher;
        public final TextView mTargetTitle;
        public final EditText mTargetBody;
        public final CardView mTargetCard;
        public final CardView mSourceCard;
        public final TabLayout mTabLayout;
        public final ImageButton mNewTabButton;
        public TextView mSourceBody;
        public ViewHolder(Context context, View v) {
            super(v);
            mMainContent = (LinearLayout)v.findViewById(R.id.main_content);
            mSourceCard = (CardView)v.findViewById(R.id.source_translation_card);
            mSourceBody = (TextView)v.findViewById(R.id.source_translation_body);
            mResourceCard = (CardView)v.findViewById(R.id.resources_card);
            mResourceLayout = (LinearLayout)v.findViewById(R.id.resources_layout);
            mTargetCard = (CardView)v.findViewById(R.id.target_translation_card);
            mTargetTitle = (TextView)v.findViewById(R.id.target_translation_title);
            mTargetBody = (EditText)v.findViewById(R.id.target_translation_body);
            mTabLayout = (TabLayout)v.findViewById(R.id.source_translation_tabs);
            mEditButton = (ImageButton)v.findViewById(R.id.edit_translation_button);
            mDoneButton = (Button)v.findViewById(R.id.done_button);
            mDoneFlag = (LinearLayout)v.findViewById(R.id.done_flag);
            ViewUtil.tintViewDrawable(mEditButton, context.getResources().getColor(R.color.dark_disabled_text));
            mTabLayout.setTabTextColors(R.color.dark_disabled_text, R.color.dark_secondary_text);
            mNewTabButton = (ImageButton) v.findViewById(R.id.new_tab_button);
        }
    }
}
