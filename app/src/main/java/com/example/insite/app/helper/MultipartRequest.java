package com.example.insite.app.helper;

/**
 * Modified by Xing Yi on 8/4/2015.
 * Source from: http://stackoverflow.com/questions/16797468/how-to-send-a-multipart-form-data-post-in-android-with-volley/28144660#28144660
 */
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.example.insite.app.model.AppSetting;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.CharsetUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequest extends Request<String> {

    MultipartEntityBuilder entity = MultipartEntityBuilder.create();
    HttpEntity httpentity;
    private String FILE_PART_NAME = "files";

    private final Response.Listener<String> mListener;
    private final File mFilePart;
    private final Map<String, String> mStringPart;
    private Map<String, String> headerParams;
    private final MultipartProgressListener multipartProgressListener;
    private long fileLength = 0L;

    public MultipartRequest(String url, Response.ErrorListener errorListener,
                            Response.Listener<String> listener, File file, long fileLength,
                            Map<String, String> mStringPart,
                            final Map<String, String> headerParams, String partName,
                            MultipartProgressListener progLitener) {
        super(Method.POST, url, errorListener);

        this.mListener = listener;
        this.mFilePart = file;
        this.fileLength = fileLength;
        this.mStringPart = mStringPart;
        this.headerParams = headerParams;
        this.FILE_PART_NAME = partName;
        this.multipartProgressListener = progLitener;

        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            entity.setCharset(CharsetUtils.get("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buildMultipartEntity();
        httpentity = entity.build();
    }

    // public void addStringBody(String param, String value) {
    // if (mStringPart != null) {
    // mStringPart.put(param, value);
    // }
    // }

    private void buildMultipartEntity() {
        entity.addPart(FILE_PART_NAME, new FileBody(mFilePart, ContentType.create("image/jpeg"), mFilePart.getName()) );
        //entity.addPart("image_file", new FileBody(mFilePart, ContentType.create("image/jpeg"), mFilePart.getName()) );
        if (mStringPart != null) {
            for (Map.Entry<String, String> entry : mStringPart.entrySet()) {
                entity.addTextBody(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String getBodyContentType() {
        return httpentity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            httpentity.writeTo(new CountingOutputStream(bos, fileLength,
                    multipartProgressListener));
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {

        try {
            //Log.d("Network Response: ", new String(response.data, "UTF-8"));
            return Response.success(new String(response.data, "UTF-8"),
                    getCacheEntry());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            // it should never happen though
            return Response.success(new String(response.data), getCacheEntry());
        }
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    //Override getHeaders() if you want to put anything in header
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = super.getHeaders();

        if (headers == null
                || headers.equals(Collections.emptyMap())) {
            headers = new HashMap<String, String>();
        }
        headers.put("Authorization", AppSetting.APItoken);
        //headers.put("Accept", "image/jpeg");

        return headers;
    }

    public static interface MultipartProgressListener {
        void transferred(long transfered, int progress);
    }

    public static class CountingOutputStream extends FilterOutputStream {
        private final MultipartProgressListener progListener;
        private long transferred;
        private long fileLength;

        public CountingOutputStream(final OutputStream out, long fileLength,
                                    final MultipartProgressListener listener) {
            super(out);
            this.fileLength = fileLength;
            this.progListener = listener;
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            if (progListener != null) {
                this.transferred += len;
                int prog = (int) (transferred * 100 / fileLength);
                this.progListener.transferred(this.transferred, prog);
            }
        }

        public void write(int b) throws IOException {
            out.write(b);
            if (progListener != null) {
                this.transferred++;
                int prog = (int) (transferred * 100 / fileLength);
                this.progListener.transferred(this.transferred, prog);
            }
        }

    }
}