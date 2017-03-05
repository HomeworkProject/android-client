/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */
/*
Modified by Sebastian Paarmann in 2015
*/
package de.s_paarmann.homeworkapp;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Calendar;


public class TimePreference extends DialogPreference {

  public static final String TAG = "TimePreference";

  private TimePicker _timeView;

  private Calendar _value;
  private boolean _isValueSet;

// region Constructors


  public TimePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }


  public TimePreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.init();
  }


  private void init() {
    _isValueSet = false;
    _value = null;
  }

// endregion

// region Event Handlers


  @Override
  protected View onCreateDialogView() {
    _timeView = new TimePicker(this.getContext());
    _timeView.setSaveFromParentEnabled(false);
    _timeView.setSaveEnabled(true);
    _timeView.setIs24HourView(DateFormat.is24HourFormat(this.getContext()));

    return _timeView;
  }


  @Override
  protected void onBindDialogView(View view) {
    super.onBindDialogView(view);

    if (_value == null) {
      Calendar now = Calendar.getInstance();
      _timeView.setCurrentHour(now.get(Calendar.HOUR_OF_DAY));
      _timeView.setCurrentMinute(now.get(Calendar.MINUTE));
    } else {
      _timeView.setCurrentHour(_value.get(Calendar.HOUR_OF_DAY));
      _timeView.setCurrentMinute(_value.get(Calendar.MINUTE));
    }
  }


  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if (defaultValue == null || !(defaultValue instanceof Long)) {
      defaultValue = 0L;
    }

    if (restorePersistedValue) {
      _value = Calendar.getInstance();
      _value.setTimeInMillis(this.getPersistedLong((Long) defaultValue));
      _isValueSet = true;
      return;
    }

    Calendar newValue = Calendar.getInstance();

    newValue.setTimeInMillis((Long) defaultValue);

    this.setValue(_value);
  }


  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);

    if (positiveResult) {
      Calendar newValue = Calendar.getInstance();

      newValue.set(Calendar.HOUR_OF_DAY, _timeView.getCurrentHour());
      newValue.set(Calendar.MINUTE, _timeView.getCurrentMinute());

      if (this.callChangeListener(newValue)) {
        this.setValue(newValue);
      }
    }
  }

// endregion

// region Getters/Setters


  public Calendar getValue() {
    return _value;
  }


  public void setValue(Calendar newValue) {
    final boolean wasChanged = !newValue.equals(_value);

    if (wasChanged || !_isValueSet) {
      _value = newValue;
      _isValueSet = true;

      this.persistLong(_value.getTimeInMillis());

      if (wasChanged) {
        this.notifyChanged();
      }
    }
  }


  @Override
  public CharSequence getSummary() {
    return (_value == null) ? null : DateFormatUtils.format(_value, "HH:mm");
  }

// endregion
}
