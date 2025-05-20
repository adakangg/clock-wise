package com.example.alarmclock;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * This class manages the fragments/tabs shown in the Main Activity's ViewPager.
 */
public class ViewPagerAdapter extends FragmentStateAdapter {
    private final SparseArray<Fragment> fragments = new SparseArray<>();

    public ViewPagerAdapter(FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AlarmsFragment();
            case 1:
                return new ConverterFragment();
            default:
                return new ThemeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public Fragment getFragment(int position) { return fragments.get(position); }
}