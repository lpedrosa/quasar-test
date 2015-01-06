package atest.client;

public interface RandomClientCallback {

    void onSuccess(int random);
    void onError(RandomClientException exception);
}
