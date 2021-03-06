package com.gmail.dleemcewen.tandemfieri.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.gmail.dleemcewen.tandemfieri.Constants.NotificationConstants;
import com.gmail.dleemcewen.tandemfieri.Entities.Delivery;
import com.gmail.dleemcewen.tandemfieri.Entities.NotificationMessage;
import com.gmail.dleemcewen.tandemfieri.Entities.Order;
import com.gmail.dleemcewen.tandemfieri.Entities.User;
import com.gmail.dleemcewen.tandemfieri.Enums.OrderEnum;
import com.gmail.dleemcewen.tandemfieri.Enums.RefundTypeEnum;
import com.gmail.dleemcewen.tandemfieri.Events.ActivityEvent;
import com.gmail.dleemcewen.tandemfieri.Json.Braintree.Transaction;
import com.gmail.dleemcewen.tandemfieri.Logging.LogWriter;
import com.gmail.dleemcewen.tandemfieri.ManageOrders;
import com.gmail.dleemcewen.tandemfieri.R;
import com.gmail.dleemcewen.tandemfieri.Utility.General;
import com.gmail.dleemcewen.tandemfieri.Repositories.Deliveries;
import com.gmail.dleemcewen.tandemfieri.Repositories.NotificationMessages;
import com.gmail.dleemcewen.tandemfieri.ViewOrderActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Ruth on 3/11/2017.
 */

public class RestaurantMainMenuExpandableListAdapter extends BaseExpandableListAdapter {

    private Activity context;
    private List<Order> orderList;
    private Map<String, List<Order>> childList;
    private DatabaseReference mDatabase;
    private TextView orderName;
    private User user;
    private ProgressDialog mDialog;
    private NotificationMessages<NotificationMessage> notifications;
    private Deliveries<Delivery> deliveries;

    public RestaurantMainMenuExpandableListAdapter(Activity context, List<Order> orderList,
                                                   Map<String, List<Order>> childList, User user) {
        this.context = context;
        this.orderList = orderList;
        this.childList = childList;
        this.user = user;

    }//end constructor

    public Object getChild(int groupPosition, int childPosition) {
        return childList.get(orderList.get(groupPosition).getKey()).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        return childList.get(orderList.get(groupPosition).getKey()).size();
    }

    public Object getGroup(int groupPosition) {
        return orderList.get(groupPosition);
    }

    public int getGroupCount() {
        return orderList.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final Order order = (Order)getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.restaurant_main_menu_group_header, null);
        }

        orderName = (TextView)convertView.findViewById(R.id.order_name);
        orderName.setTypeface(null, Typeface.BOLD);
        orderName.setText(order.toString());
        orderName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //LogWriter.log(context, Level.INFO, "This should open the order");
                Intent intent = new Intent(context, ViewOrderActivity.class);
                Bundle orderBundle = new Bundle();
                orderBundle.putSerializable("Order", order);
                intent.putExtras(orderBundle);
                context.startActivity(intent);
            }
        });

        return convertView;
    }

    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final Order selectedOrder = (Order) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.restaurant_main_menu_list_item, null);
        }

        BootstrapButton manage_button = (BootstrapButton) convertView.findViewById(R.id.manage_button);
        manage_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("Order", selectedOrder);
                bundle.putSerializable("Owner", user);
                Intent intent = new Intent(context, ManageOrders.class);
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });

        BootstrapButton refund_button = (BootstrapButton) convertView.findViewById(R.id.refund_button);
        refund_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refundDialog(selectedOrder, user);
            }
        });

        return convertView;
    }//end get child view

    private void refundDialog(final Order order, final User user) {
        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.dialog_restaurant_refund, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(view);

        final EditText comments = (EditText) view.findViewById(R.id.et_refund_comment);

        alertDialogBuilder
                .setCancelable(false)
                .setMessage(context.getString(R.string.confirm_refund))
                .setPositiveButton(context.getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (General.isEditTextEmpty(comments)) {
                                    Toast.makeText(context, "Please enter the refund reason.", Toast.LENGTH_LONG).show();
                                    dialog.cancel();
                                    return;
                                }

                                mDialog = new ProgressDialog(context);
                                mDialog.setMessage("Processing refund!");
                                mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                mDialog.show();

                                findTransaction(order, user, comments);
                            }
                        })
                .setNegativeButton(context.getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void findTransaction(final Order order, final User user, final TextView reason) {
        if (order.getStatus() == OrderEnum.PAYMENT_PENDING) {
            Toast.makeText(context, "Unable to refund. Payment never submitted.", Toast.LENGTH_LONG).show();
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        params.put("transactionID", order.getBraintreeTransactionId());

        String url = context.getString(R.string.braintreeBaseURL) + "findtransaction";

        //Start rest api
        client.post(context
                ,url
                ,params
                ,new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject resp) {
                        String json = resp.toString();

                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.create();

                        Transaction detail = gson.fromJson(json, Transaction.class);

                        if (detail.success.equals("true")) {
                            RefundTypeEnum type;

                            Log.v("Braintree", detail.status);

                            switch (detail.status.toLowerCase()) {
                                case "settled":
                                case "settling":
                                    type = RefundTypeEnum.REFUND;
                                    break;
                                case "authorized":
                                case "submitted_for_settlement":
                                    type = RefundTypeEnum.VOID;
                                    break;
                                default:
                                    Toast.makeText(context,
                                            "This order is not in a status that can be refunded/voided.",
                                            Toast.LENGTH_LONG).show();

                                    mDialog.hide();
                                    return;
                            }

                            Log.v("Braintree", "processrefund");

                            boolean isENROUTE = (order.getStatus() == OrderEnum.EN_ROUTE) ? true : false;

                            processRefund(type,
                                    detail.transactionID,
                                    user.getAuthUserID(),
                                    order,
                                    isENROUTE,
                                    reason);
                        } else {
                            Log.v("Braintree:", "FindTransaction: " + detail.error);
                            mDialog.hide();

                            Toast.makeText(context,
                                    "Unable to find transaction.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(int status, Header[] header, Throwable t, JSONObject obj) {
                        mDialog.hide();
                        Toast.makeText(context,
                                "Unable to locate payment information at this time.",
                                Toast.LENGTH_LONG).show();

                        Log.v("Braintree", "FindTransactionFailed: " + t.getMessage().toString());
                    }
                });
        //End rest api
    }

    private void processRefund(RefundTypeEnum type, String transactionID, final String ownerID,
                               final Order order, final boolean isENROUTE, final TextView reason) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        params.put("transactionID", transactionID);
        Log.v("Braintree", "TransactionID: " + transactionID);

        String url = context.getString(R.string.braintreeBaseURL);

        url += (type == RefundTypeEnum.REFUND) ? "refundtransaction" : "voidtransaction";

        //Start rest api
        client.post(context
                ,url
                ,params
                ,new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int status, Header[] headers, JSONObject resp) {
                        String json = resp.toString();

                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.create();

                        Transaction detail = gson.fromJson(json, Transaction.class);

                        if (detail.success.equals("true")) {
                            Log.v("Braintree", "Order refunded");

                            mDatabase = FirebaseDatabase.getInstance().getReference();
                            mDatabase.child("Order").child(ownerID).child(order.getOrderId()).child("status").setValue(OrderEnum.REFUNDED);
                            mDatabase.child("Order").child(ownerID).child(order.getOrderId()).child("refundReason").setValue(reason.getText().toString());

                            //set status and refund reason in the order object as well
                            order.setStatus(OrderEnum.REFUNDED);
                            order.setRefundReason(reason.getText().toString());

                            LogWriter.log(context, Level.FINE, "sending notification to diner");

                            notifications = new NotificationMessages<>(context);

                            //Send refunded notification to diner
                            notifications
                                .sendNotification(NotificationConstants.Action.REMOVED, order, order.getCustomerId());

                            if (isENROUTE) {
                                //Send Notification to Driver
                                DatabaseReference deliveryDatabase = FirebaseDatabase.getInstance().getReference("Delivery");
                                deliveryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        deliveries = new Deliveries<>(context);
                                        String driverId = deliveries.findDriverIdForOrder(order.getOrderId(), dataSnapshot);

                                        notifications = new NotificationMessages<>(context);
                                        notifications.sendNotification(NotificationConstants.Action.REMOVED, order, driverId);

                                        //clean up delivery and delivery location in firebase
                                        mDatabase.child("Delivery").child(driverId).child("Order").child(order.getCustomerId()).child(order.getOrderId()).removeValue();
                                        mDatabase.child("Delivery").child(driverId).child("currentOrderId").removeValue();
                                        mDatabase.child("Delivery Location").child(order.getCustomerId()).removeValue();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        //not implemented
                                    }
                                });
                            }

                            EventBus.getDefault()
                                    .post(new ActivityEvent(ActivityEvent.Result.REFRESH_RESTAURANT_MAIN_MENU));

                            Toast.makeText(context,
                                    "Order refunded.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Log.v("Braintree:", "RefundTransaction: " + detail.error);

                            Toast.makeText(context,
                                    "Unable to refund order.",
                                    Toast.LENGTH_LONG).show();
                        }

                        mDialog.hide();
                    }

                    @Override
                    public void onFailure(int status, Header[] header, Throwable t, JSONObject obj) {
                        mDialog.hide();
                        Toast.makeText(context,
                                "Unable to refund order at this time. Check internet connection.",
                                Toast.LENGTH_LONG).show();

                        Log.v("Braintree", "RefundTransactionFailed: " + t.getMessage().toString());
                    }
                });
        //End rest api
    }
}//end class