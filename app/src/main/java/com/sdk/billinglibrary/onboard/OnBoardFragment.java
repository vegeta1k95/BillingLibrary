package com.sdk.billinglibrary.onboard;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.sdk.billinglibrary.R;

public class OnBoardFragment extends Fragment {

    @Keep
    public OnBoardFragment() { super(); }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        String layoutName = getArguments().getString("layout");
        int layoutId = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
        View layout = inflater.inflate(layoutId, container, false);

        int checkBoxId = context.getResources().getIdentifier("chk_onboard_consent", "id", context.getPackageName());

        if (checkBoxId != 0) {
            CheckBox consent = layout.findViewById(checkBoxId);
            if (consent != null) {
                Button btnContinue = getActivity().findViewById(R.id.btn_continue);
                btnContinue.setEnabled(true);
                consent.setOnCheckedChangeListener((buttonView, isChecked) -> btnContinue.setEnabled(isChecked));

                int tvLinkId = context.getResources().getIdentifier("onboard_descr", "id", context.getPackageName());
                if (tvLinkId != 0) {
                    TextView tvLink = layout.findViewById(tvLinkId);
                    if (tvLink != null) {
                        tvLink.setMovementMethod(LinkMovementMethod.getInstance());
                        String text = tvLink.getText().toString().replace("\n", "<br>");
                        TypedValue privacy = new TypedValue();
                        context.getTheme().resolveAttribute(R.attr.billing_default_privacy, privacy, true);
                        String url = privacy.coerceToString().toString();
                        String link = "<a href=\"" + url + "\">" + text + "</a>";
                        tvLink.setText(HtmlCompat.fromHtml(link, HtmlCompat.FROM_HTML_MODE_LEGACY));
                    }
                }

            }
        }

        return layout;
    }

    static OnBoardFragment getInstance(String layoutName) {
        OnBoardFragment fragment = new OnBoardFragment();
        Bundle params = new Bundle();
        params.putString("layout", layoutName);
        fragment.setArguments(params);
        return fragment;
    }

}
