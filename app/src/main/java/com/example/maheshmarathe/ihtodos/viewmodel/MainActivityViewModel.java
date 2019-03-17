package com.example.maheshmarathe.ihtodos.viewmodel;

import android.arch.lifecycle.ViewModel;

import com.example.maheshmarathe.ihtodos.view.MainActivity;

/**
 * ViewModel for {@link MainActivity}.
 */

public class MainActivityViewModel extends ViewModel {

    private boolean mIsSigningIn;

    public MainActivityViewModel() {
        mIsSigningIn = false;
    }

    public boolean getIsSigningIn() {
        return mIsSigningIn;
    }

    public void setIsSigningIn(boolean mIsSigningIn) {
        this.mIsSigningIn = mIsSigningIn;
    }
}
