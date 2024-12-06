package tech.amikos.chromadb;

import java.util.Map;

public abstract class WithParam {
    public abstract void apply(Map<String, Object> params) throws ChromaException;

    public static WithParam baseAPI(String baseApi) {
        return new WithBaseAPI(baseApi);
    }
}


class WithBaseAPI extends WithParam {
    private final String baseAPI;

    public WithBaseAPI(String baseAPI) {
        this.baseAPI = baseAPI;
    }

    @Override
    public void apply(Map<String, Object> params) {
        params.put(Constants.EF_PARAMS_BASE_API, baseAPI);
    }
}

class WithTimeout extends WithParam {
    private final int timeout;

    public WithTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void apply(Map<String, Object> params) throws ChromaException {
        if (timeout < 0) {
            throw new ChromaException("Timeout must be a positive integer");
        }
//        params.put(Constants.EF_PARAMS_TIMEOUT, timeout);
    }
}
