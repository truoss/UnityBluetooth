package de.truoss.unitybluetoothplugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

import android.R.integer;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import com.unity3d.player.*;

// TODO:
// bluetooth on / off

public class UnityBluetoothAdapter 
{
	private BluetoothAdapter mBluetoothAdapter;
	private ConnectedThread mBluetoothConnection;
	
	public void getDefaultAdapter()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		startDiscovery();
		//registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}	
	
	public boolean isEnabled()
	{
		return mBluetoothAdapter.isEnabled();
	}
	
	public boolean enable()
	{
		return mBluetoothAdapter.enable();
	}
	
	public boolean disable()
	{
		return mBluetoothAdapter.disable();
	}
	
	public String[] getBoundDeviceNames()
	{
		if(mBluetoothAdapter == null)
			getDefaultAdapter();
		
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		
		ArrayList<String> list = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices)
            list.add(bt.getName());
        
        String[] stockArr = new String[list.size()];
        stockArr = list.toArray(stockArr);
        
        list.clear();
        pairedDevices.clear();
		return stockArr;
	}	
	
	public boolean startDiscovery()
	{
		return mBluetoothAdapter.startDiscovery();				
	}
	
	public boolean cancelDiscovery()
	{
		return mBluetoothAdapter.cancelDiscovery();
	}
	
	public boolean isDiscovering()
	{
		return mBluetoothAdapter.isDiscovering();
	}
	
	public void send(String msg)
	{
		if(mBluetoothConnection != null)
			mBluetoothConnection.write(msg);
	}
	
	public void disconnect()
	{
		if(mBluetoothConnection != null){
			mBluetoothConnection.disconnect();
			mBluetoothConnection.forceStop = true;
			mBluetoothConnection = null;
		}			
	}
	
	public void connect(String deviceName)
	{		
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		
		int idx = -1;
		for (BluetoothDevice bDevice : pairedDevices) {
			idx++;
			if(bDevice.getName() == deviceName)
			{
				break;
			}
		}
		
		if(idx == -1)
		{
			UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnConnectionFailed", "No devices with name: " + deviceName);
			return;
		}
		
		BluetoothDevice mBluetoothDevice = (BluetoothDevice) pairedDevices.toArray()[idx];
		new ConnectThread(mBluetoothDevice).start();
	}
		
	class ConnectThread extends Thread
	{
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;

	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;

	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            ParcelUuid[] uuids = mmDevice.getUuids();
	            tmp = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
	        } catch (IOException e) { 
	        	UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnIOException", e.toString());
	        }
	        mmSocket = tmp;
	    }

	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();

	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            connectionFailed();
	            return;
	        }
	        
	        // start thread for satble connection
	        mBluetoothConnection = new ConnectedThread(mmSocket);
	        mBluetoothConnection.start();
	    }

	    
	    private void connectionFailed()
	    {
	    	UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnConnectionFailed", "failed");
	    }
	}

	static class ConnectedThread extends Thread
	{
	    private final BluetoothSocket mmSocket;
	    private final OutputStream mmOutStream;
	    private final InputStream mmInStream;

	    boolean forceStop = false;

	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;

	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) {
	        	UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnIOException", e.toString());
	        }

	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;

	        UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnConnected", "connected");
	    }

	    public void run()
	    {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (!forceStop) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnMsgReceived", Integer.toString(bytes));
	            } 
	            catch (IOException e)
	            {
	                connectionLost();
	                break;
	            }
	        }
	    }

	    
	    public void write(String str)
	    {
	        try {
	            mmOutStream.write(str.getBytes());
	        } catch (IOException e) {
	        	UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnIOException", e.toString());
	        }
	    }

	    private void connectionLost()
	    {
	    	UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnConnectionLost", "lost");	    	
	    }
	    
	    public void disconnect()
	    {
	        //close connection
	        if(mmSocket != null) {
	            try {
	                mmSocket.close();
	            } catch (IOException e) {
	            }
	        }
	        UnityPlayer.UnitySendMessage("UnityBluetoothAdapter", "OnDisconnect", "disconnect");
	    }
	}		
}


