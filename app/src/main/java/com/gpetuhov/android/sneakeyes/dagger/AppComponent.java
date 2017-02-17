package com.gpetuhov.android.sneakeyes.dagger;

import com.gpetuhov.android.sneakeyes.ButtonActivity;
import com.gpetuhov.android.sneakeyes.SneakingService;

import javax.inject.Singleton;

import dagger.Component;

// Dagger component tells, into which classes instances instantiated by Module will be injected.
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(SneakingService sneakingService);
    void inject(ButtonActivity buttonActivity);
}