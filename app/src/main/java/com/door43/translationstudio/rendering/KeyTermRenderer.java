package com.door43.translationstudio.rendering;

import android.text.TextUtils;

import com.door43.translationstudio.projects.Frame;
import com.door43.translationstudio.projects.Term;
import com.door43.translationstudio.spannables.Span;
import com.door43.translationstudio.spannables.TermSpan;

import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class renders
 */
public class KeyTermRenderer extends RenderingEngine {
    private final List<Term> mTerms;
    private final Frame mFrame;
    private final Span.OnClickListener mClickListener;

    /**
     * Creates a new key term renderer
     * @param frame the frame with key terms that will be rendered
     */
    public KeyTermRenderer(Frame frame, Span.OnClickListener clickListener) {
        mClickListener = clickListener;
        mTerms = frame.getChapter().getProject().getTerms();
        mFrame = frame;
    }

    @Override
    public CharSequence render(CharSequence in) {
        // locate key terms
        CharSequence keyedText = in;
        Vector<Boolean> indicies = new Vector<Boolean>();
        indicies.setSize(keyedText.length());
        for(Term t:mTerms) {
            if(isStopped()) return in;
            StringBuffer buf = new StringBuffer();
            Pattern p = Pattern.compile("\\b" + t.getName() + "\\b");
            // TRICKY: we need to run two matches at the same time in order to keep track of used indicies in the string
            Matcher matcherSourceText = p.matcher(in);
            Matcher matcherKeyedText = p.matcher(keyedText);

            while (matcherSourceText.find() && matcherKeyedText.find()) {
                if(isStopped()) return in;
                // ensure the key term was found in an area of the string that does not overlap another key term.
                if(indicies.get(matcherSourceText.start()) == null && indicies.get(matcherSourceText.end()) == null) {
                    // build important terms list.
                    mFrame.addImportantTerm(matcherSourceText.group());
                    // build the term
                    String key = "<keyterm>" + matcherSourceText.group() + "</keyterm>";
                    // lock indicies to prevent key term collisions
                    for(int i = matcherSourceText.start(); i <= matcherSourceText.end(); i ++) {
                        if(isStopped()) return in;
                        indicies.set(i, true);
                    }

                    // insert the key into the keyedText
                    matcherKeyedText.appendReplacement(buf, key);
                } else {
                    // do nothing. this is a key collision
                    // e.g. the key term "life" collided with "eternal life".
                }
            }
            matcherKeyedText.appendTail(buf);
            keyedText = buf.toString();
        }

        // convert links into spans
        CharSequence out = "";
        int lastIndex = 0;
        if(keyedText != null) {
            // TODO: will converting the keyed text to string cause any problems with already generated spans?
            Pattern p = Pattern.compile("<keyterm>(((?!</keyterm>).)*)</keyterm>", Pattern.DOTALL);
            Matcher matcherKeys = p.matcher(keyedText);
            while(matcherKeys.find()) {
                if(isStopped()) return in;
                TermSpan term = new TermSpan(matcherKeys.group(1), matcherKeys.group(1));
                term.setOnClickListener(mClickListener);

                out = TextUtils.concat(out, keyedText.subSequence(lastIndex, matcherKeys.start()), term.toCharSequence());
                lastIndex = matcherKeys.end();
            }
            out = TextUtils.concat(out, keyedText.subSequence(lastIndex, keyedText.length()));
        }
        return out;
    }
}
