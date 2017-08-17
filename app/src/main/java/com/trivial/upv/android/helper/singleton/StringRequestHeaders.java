package com.trivial.upv.android.helper.singleton;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;

import static com.android.volley.toolbox.HttpHeaderParser.parseDateAsEpoch;

/**
 * Created by jvg63 on 16/08/2017.
 */

public class StringRequestHeaders extends Request<String> {
    private final Response.Listener<String> mListener;

    /**
     * Creates a new request with the given method.
     *
     * @param method        the request {@link Method} to use
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public StringRequestHeaders(int method, String url, Response.Listener<String> listener,
                                Response.ErrorListener errorListener, boolean validateBOM) {
        super(method, url, errorListener);
        mListener = listener;
        mDateLastModified = 0;
        mValidateBOM = validateBOM;
    }


    private long mDateLastModified;
    private boolean mValidateBOM;

    /**
     * Creates a new GET request.
     *
     * @param url           URL to fetch the string at
     * @param listener      Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     * @param validateBOM
     */
    public StringRequestHeaders(String url, Response.Listener<String> listener, Response.ErrorListener errorListener, boolean validateBOM) {
        this(Method.GET, url, listener, errorListener, validateBOM);
    }



    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed = null;

        if (mValidateBOM) {
            try {
                parsed = new String(response.data, "UTF-8");

                final String UTF8_BOM = "\uFEFF";

                if (!parsed.startsWith(UTF8_BOM)) {
                    parsed = new String(response.data, "windows-1252");
                }

            } catch (UnsupportedEncodingException e) {
                parsed = new String(response.data);
            }
        }else {
            try {
                parsed = new String(response.data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                parsed = new String(response.data);
            }
        }

        String headerValue = response.headers.get("Last-Modified");
        if (headerValue != null) {
            mDateLastModified = parseDateAsEpoch(headerValue);
        } else {
//            dateLastModified = new Date().getTime();
            mDateLastModified = -1;
        }

        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

    public long getDateLastModified() {
        return mDateLastModified;
    }
}
