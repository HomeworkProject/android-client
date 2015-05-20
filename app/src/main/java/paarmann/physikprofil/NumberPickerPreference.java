/*
 * Credit for the original version goes to jackos2500 (https://gist.github.com/jackos2500/9997104)
 * License for modifications:
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * A {@code DialogPreference} for picking a number between a specified maximum and minimum value.
 */
public class NumberPickerPreference extends DialogPreference {

  private int min, max;
  private NumberPicker picker;
  private Integer initialValue;

  public NumberPickerPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray
        pickerType =
        context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
    min = pickerType.getInt(R.styleable.NumberPickerPreference_minValue, 1);
    max = pickerType.getInt(R.styleable.NumberPickerPreference_maxValue, 10);
    pickerType.recycle();
    setDialogLayoutResource(R.layout.number_pref);
  }

  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);
    this.picker = (NumberPicker) view.findViewById(R.id.pref_num_picker);
    picker.setMinValue(min);
    picker.setMaxValue(max);
    if (this.initialValue != null) {
      picker.setValue(initialValue);
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    super.onClick(dialog, which);
    if (which == DialogInterface.BUTTON_POSITIVE) {
      this.initialValue = picker.getValue();
      persistInt(initialValue);
      callChangeListener(initialValue);
    }
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    int
        def =
        (defaultValue instanceof Number) ? (Integer) defaultValue : (defaultValue != null) ? Integer
            .parseInt(defaultValue.toString()) : 1;
    if (restorePersistedValue) {
      this.initialValue = getPersistedInt(def);
    } else {
      this.initialValue = (Integer) defaultValue;
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInt(index, 1);
  }
}
