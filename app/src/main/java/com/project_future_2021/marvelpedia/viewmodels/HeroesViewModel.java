package com.project_future_2021.marvelpedia.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.project_future_2021.marvelpedia.BuildConfig;
import com.project_future_2021.marvelpedia.R;
import com.project_future_2021.marvelpedia.data.Hero;
import com.project_future_2021.marvelpedia.repositories.HeroRepository;
import com.project_future_2021.marvelpedia.singletons.VolleySingleton;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.List;

public class HeroesViewModel extends AndroidViewModel {
    //TODO have that in a constants class...
    private static final String TAG = "HeroesViewModel";
    private static final String REQUEST_TAG = "HeroesFragmentRequest";

    private final HeroRepository heroRepository;
    // 'vm' for viewmodel
    private final MediatorLiveData<List<Hero>> vmAllHeroesCombined;
    private final MutableLiveData<List<Hero>> vmServerHeroes;
    private final LiveData<List<Hero>> vmDbHeroes;

    private final MutableLiveData<List<Hero>> vmListOfHeroesTheUserSearchedFor;

    private final MutableLiveData<Boolean> vmIsLoading;
    private final String vmUrl;
    private final String request_type = "/v1/public/characters";
    // TODO change this, according to Server's Api Instructions
    private final String request_type_for_search = "/v1/public/characters";
    private final String urlForSearch;

    public HeroesViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "HeroesViewModel: constructor");

        vmIsLoading = new MutableLiveData<>();

        vmUrl = createUrlForApiCall(request_type);

        urlForSearch = createUrlForApiCall(request_type_for_search);


        heroRepository = new HeroRepository(application, vmUrl);

        vmAllHeroesCombined = new MediatorLiveData<>();

        vmDbHeroes = heroRepository.getRepoDbHeroes();
        vmServerHeroes = heroRepository.getRepoServerHeroes();

        vmListOfHeroesTheUserSearchedFor = heroRepository.getRepoListOfHeroesTheUserSearchedFor();

        // when new heroes are fetched from the Server, only add them to the DB,
        // [SOS] We will only be observing the DB for changes.
        vmAllHeroesCombined.addSource(vmServerHeroes, newHeroListFromServer -> {
            Log.d(TAG, "HeroesViewModel: newHeroListFromServer");
            insertManyHeroes(newHeroListFromServer);
        });

        // since we are observing the DB,
        // if we get new Heroes from the Server ->
        //      we add them to the DB ->
        //          we show a refreshed list with Heroes from the DB.
        vmAllHeroesCombined.addSource(vmDbHeroes, newHeroListFromDb -> {
            Log.d(TAG, "HeroesViewModel: newHeroListFromDb");
            vmAllHeroesCombined.postValue(newHeroListFromDb);
        });

        // Also, observe the isLoading LiveData variable that is in the Repository.
        // It is true while new Heroes are being fetched, false otherwise.
        vmAllHeroesCombined.addSource(heroRepository.getRepoIsLoading(), newState -> {
            if (newState) {
                Log.d(TAG, ".....LOADING.....: ");
                vmIsLoading.postValue(true);
            } else {
                Log.d(TAG, ".....DONE LOADING.....: ");
                vmIsLoading.postValue(false);
            }
        });
    }

    public LiveData<List<Hero>> getVmServerHeroes() {
        return vmServerHeroes;
    }

    public LiveData<List<Hero>> getVmDbHeroes() {
        return vmDbHeroes;
    }

    public LiveData<Boolean> getVmIsLoading() {
        return vmIsLoading;
    }

    public LiveData<List<Hero>> getVmAllHeroesCombined() {
        return vmAllHeroesCombined;
    }

    public LiveData<List<Hero>> getVmListOfHeroesTheUserSearchedFor() {
        return vmListOfHeroesTheUserSearchedFor;
    }

    public void insertHero(Hero hero) {
        heroRepository.insert(hero);
    }

    public void insertManyHeroes(List<Hero> heroes) {
        heroRepository.insertManyHeroes(heroes);
    }

    public void updateHero(Hero hero) {
        // heroRepository.update(hero); <- That 'simple' approach could lead to bugs.
        // TODO maybe comment/uncomment that, but it is considered better practice. (?)
        Hero copyOfInputHero = Hero.copyHero(hero);
        heroRepository.update(copyOfInputHero);
    }

    public void deleteHeroWithId(int heroId) {
        heroRepository.deleteHeroWithId(heroId);
    }

    public void deleteAllHeroes() {
        heroRepository.deleteAllHeroes();
    }

    public HeroRepository getHeroRepository() {
        return heroRepository;
    }

    public void searchForHeroesWithName(String heroName, String REQUEST_TAG) {
        heroRepository.RepoSearchForHeroesWithName(getApplication().getBaseContext(), urlForSearch, REQUEST_TAG, heroName);
    }

    public String createUrlForApiCall(String request_type) {
        String timestamp_now = getNow();
        String BASE_URL = getApplication().getString(R.string.base_url);
        String PRIVATE_API_KEY = BuildConfig.PRIVATE_API_KEY;
        String PUBLIC_API_KEY = BuildConfig.PUBLIC_API_KEY;

        String hashInput = timestamp_now + PRIVATE_API_KEY + PUBLIC_API_KEY;
        String hashResult = generateMD5HashFromString(hashInput);

        String final_url = BASE_URL + request_type + "?ts=" + timestamp_now + "&apikey=" + PUBLIC_API_KEY + "&hash=" + hashResult;

        Log.d(TAG, "createUrlForApiCall: Url is: " + final_url);
        return final_url;
    }

    public void loadMore() {
        // We want the next batch of data
        String newUrl = createUrlForApiCall(request_type);
        heroRepository.RepoLoadMore(getApplication().getBaseContext(), newUrl);
    }

    private String getNow() {
        long datetime = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(datetime);
        return timestamp.toString();
    }

    // Google our friend and savior :)
    private String generateMD5HashFromString(String inputString) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (md5 != null) {
            md5.update(StandardCharsets.UTF_8.encode(inputString));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } else {
            return "";
        }
    }

    public boolean cancelRequestWithTag(String requestTag) {
        if (VolleySingleton.getInstance(getApplication().getBaseContext()) != null) {
            VolleySingleton.getInstance(getApplication().getBaseContext()).getRequestQueue().cancelAll(requestTag);
            heroRepository.stopLoading();
            return true;
        } else {
            return false;
        }
    }
}