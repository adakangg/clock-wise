package com.example.alarmclock;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * This custom input filter class is used to restrict the range of values accepted by an EditText element.
 */

public class InputFilterMinMax implements InputFilter {
    private final int min, max;

    public InputFilterMinMax(String min, String max) {
        this.min = Integer.parseInt(min);
        this.max = Integer.parseInt(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            // concatenate current text with input
            String newValue = dest.toString().substring(0, dstart) + source.toString() + dest.toString().substring(dend);
            int input = Integer.parseInt(newValue);
            if (input >= min && input <= max)  return null; // accept input
        } catch (NumberFormatException e) { // continue to reject input
        }
        return ""; // reject input
    }
}
