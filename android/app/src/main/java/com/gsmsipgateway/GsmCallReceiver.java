package com.gsmsipgateway;

import android.content.*;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class GsmCallReceiver extends BroadcastReceiver {
    private static final String TAG = "GsmCallReceiver";
    private static final Map<Integer, String> LAST_STATE_BY_SLOT = new HashMap<>();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        
        // Phát hiện SIM slot (Dual SIM support)
        int simSlot = detectSimSlot(context, intent);
        
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            Intent si = new Intent(context, GsmSipBridgeService.class);
            si.setAction("ACTION_INCOMING_CALL");
            si.putExtra("caller_number", number != null ? number : "Unknown");
            si.putExtra("sim_slot", simSlot);
            Log.d(TAG, "Incoming call from " + number + " on SIM slot " + simSlot);
            context.startForegroundService(si);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            Log.d(TAG, "Call OFFHOOK on SIM slot " + simSlot);
        }

        String prevState = LAST_STATE_BY_SLOT.get(simSlot);
        LAST_STATE_BY_SLOT.put(simSlot, state);

        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)
                && (TelephonyManager.EXTRA_STATE_RINGING.equals(prevState)
                || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(prevState))) {
            Intent si = new Intent(context, GsmSipBridgeService.class);
            si.setAction("ACTION_CALL_ENDED");
            si.putExtra("sim_slot", simSlot);
            Log.d(TAG, "Call ended on SIM slot " + simSlot);
            context.startForegroundService(si);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            Log.d(TAG, "Ignoring IDLE without active previous state on SIM slot " + simSlot);
        }
    }

    /**
     * Phát hiện SIM slot nào gọi vào
     * Slot 0 (SIM1) -> Sử dụng SIP 1001
     * Slot 1 (SIM2) -> Sử dụng SIP 1002
     */
    private int detectSimSlot(Context context, Intent intent) {
        try {
            // Trên Android 5.1+ (API 22+): sử dụng SubscriptionManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // Lấy subscription ID từ intent (nếu có)
                if (intent.hasExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX)) {
                    int subId = intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, 0);
                    int slot = SubscriptionManager.getSlotIndex(subId);
                    if (slot >= 0) {
                        Log.d(TAG, "Detected SIM slot from intent: " + slot);
                        return slot;
                    }
                }

                // Fallback: Kiểm tra cuộc gọi hiện tại trên mỗi slot
                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(
                    Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    java.util.List<SubscriptionInfo> activeSubs = sm.getActiveSubscriptionInfoList();
                    if (activeSubs != null) {
                        for (SubscriptionInfo subInfo : activeSubs) {
                            int slot = subInfo.getSimSlotIndex();
                            if (slot >= 0) {
                                Log.d(TAG, "Active SIM found at slot: " + slot);
                                // Chọn slot thứ nhất có sẵn
                                return slot;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error detecting SIM slot: " + e.getMessage());
        }

        // Default: SIM slot 0
        return 0;
    }
}
