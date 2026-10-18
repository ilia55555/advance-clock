package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Owns document-picker results, including after the host is recreated. */
public final class ToolImagePickerFragment extends Fragment {
    static void pick(Activity host, String kind, int slot) {
        String tag = "tool_image_" + kind + slot;
        ToolImagePickerFragment fragment = (ToolImagePickerFragment)
                host.getFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new ToolImagePickerFragment();
            Bundle args = new Bundle();
            args.putString("kind", kind);
            args.putInt("slot", slot);
            fragment.setArguments(args);
            host.getFragmentManager().beginTransaction().add(fragment, tag).commit();
            host.getFragmentManager().executePendingTransactions();
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { fragment.startActivityForResult(intent, 1); }
        catch (android.content.ActivityNotFoundException ignored) {
            android.widget.Toast.makeText(host, R.string.tool_image_unavailable,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 1 || result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Activity host = getActivity();
        if (host == null) return;
        Uri uri = data.getData();
        try {
            host.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
        host.getSharedPreferences("time_tools", Activity.MODE_PRIVATE).edit()
                .putString(getArguments().getString("kind") + "_image"
                        + getArguments().getInt("slot"), uri.toString()).apply();
    }
}
