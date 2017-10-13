package com.example.joseph.sweepersd.archived.presentation.manualalarms;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.joseph.sweepersd.R;

/**
 * Created by joseph on 5/18/17.
 */

public class CreateAlarmLabelDialogFragment extends DialogFragment {

    private CreateAlarmLabelDialogListener mListener;
    private EditText mEditText;

    public interface CreateAlarmLabelDialogListener {
        void onLabelCreated(String label);
    }

    public static CreateAlarmLabelDialogFragment newInstance(
            CreateAlarmLabelDialogListener listener) {
        CreateAlarmLabelDialogFragment fragment = new CreateAlarmLabelDialogFragment();
        fragment.setListener(listener);

        return fragment;
    }

    public CreateAlarmLabelDialogFragment() {

    }

    private void setListener(CreateAlarmLabelDialogListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_create_label);
        dialog.setTitle("Create a Label");

        mEditText = (EditText) dialog.findViewById(R.id.edittext_label);

        Button okButton = (Button) dialog.findViewById(R.id.button_ok);
        // if button is clicked, close the custom dialog
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String label = mEditText.getText().toString();
                mListener.onLabelCreated(label);

                dialog.dismiss();
            }
        });

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        mEditText.requestFocus();
    }
}
