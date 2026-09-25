package com.uniqueruralfund.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

/**
 * Unique Rural Fund Android wrapper.
 *
 * The site's "UPI-তে পাঠান" button hands off to the user's UPI app via
 * window.location.href = 'upi://pay?...'. A plain WebView does not know how
 * to resolve non-http(s) URI schemes, so we install a custom WebViewClient
 * that catches any external scheme (upi:, tel:, mailto:, intent:, etc.) and
 * routes it through a normal Android ACTION_VIEW intent instead, letting the
 * OS launch the matching app (or show its chooser). Ordinary http(s)
 * navigation is left untouched by delegating to Capacitor's own
 * BridgeWebViewClient/Bridge logic.
 */
public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.bridge.getWebView().setWebViewClient(new BridgeWebViewClient(this.bridge) {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(
                            getApplicationContext(),
                            "কোনো UPI অ্যাপ পাওয়া যায়নি / No app found to open this link",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                    // We handled it (or showed an error) - don't let the WebView try to load it.
                    return true;
                }

                // Normal http(s) navigation: let Capacitor's own bridge handle it as usual.
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
    }
}
