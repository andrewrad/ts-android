package com.door43.translationstudio.newui.publish;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;
import com.door43.widget.ViewUtil;

import java.security.InvalidParameterException;

/**
 * Created by joel on 9/20/2015.
 */
public class ReviewFragment extends PublishStepFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_publish_review, container, false);

        Bundle args = getArguments();
        String targetTranslationId = args.getString(PublishActivity.EXTRA_TARGET_TRANSLATION_ID);
        String sourceTranslationId = args.getString(ARG_SOURCE_TRANSLATION_ID);
        if (targetTranslationId == null) {
            throw new InvalidParameterException("a valid target translation id is required");
        }
        if (sourceTranslationId == null) {
            throw new InvalidParameterException("a valid source translation id is required");
        }

        Library library = AppContext.getLibrary();
        Translator translator = AppContext.getTranslator();

        SourceTranslation sourceTranslation = library.getSourceTranslation(sourceTranslationId);
        TargetTranslation targetTranslation = translator.getTargetTranslation(targetTranslationId);

        // project title
        TextView projectTitle = (TextView)rootView.findViewById(R.id.project_title);
        String title = sourceTranslation.getProjectTitle() + " - " + targetTranslation.getTargetLanguageName();
        projectTitle.setText(String.format(getResources().getString(R.string.project_title_complex), title));

        // color icons
        ImageView projectIcon = (ImageView)rootView.findViewById(R.id.project_icon);
        ViewUtil.tintViewDrawable(projectIcon, getResources().getColor(R.color.green));
        ImageView profileIcon = (ImageView)rootView.findViewById(R.id.profile_icon);
        ViewUtil.tintViewDrawable(profileIcon, getResources().getColor(R.color.green));
        ImageView licenseIcon = (ImageView)rootView.findViewById(R.id.license_icon);
        ViewUtil.tintViewDrawable(licenseIcon, getResources().getColor(R.color.green));
        ImageView guidelinesIcon = (ImageView)rootView.findViewById(R.id.guidelines_icon);
        ViewUtil.tintViewDrawable(guidelinesIcon, getResources().getColor(R.color.green));
        ImageView faithIcon = (ImageView)rootView.findViewById(R.id.faith_icon);
        ViewUtil.tintViewDrawable(faithIcon, getResources().getColor(R.color.green));

        // next button
        Button nextButton = (Button)rootView.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().nextStep();
            }
        });

        return rootView;
    }
}