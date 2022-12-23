package com.example.test;

import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import engine.Engine;

public class MyVpnService extends VpnService {
    private static final String ADDRESS = "10.0.0.2";
    private static final String ROUTE = "0.0.0.0";
    private static final String DNS = "1.1.1.1";
    private static final String SOCKS5 = "socks5://10.88.111.24:8080"; // Your SOCKS5 server

    // You could spin up a local SOCK5 server on your workstation with:
    // ssh -ND "*:8080" -q -C -N <username>@<remote-host>

    private final ExecutorService executors = Executors.newFixedThreadPool(1);
    private ParcelFileDescriptor tun;

    @Override
    public void onCreate() {
        super.onCreate();

        if (tun == null) {
            try {
                Builder builder = new Builder()
                        .addAddress(ADDRESS, 24)
//                        .addRoute(ROUTE, 0)
                        .addDnsServer(DNS)
                        .addDisallowedApplication(this.getApplication().getPackageName());

                // let DNS queries bypass VPN if SOCKS server does not support UDP bind
                addRoutesExcept(builder, DNS, 32);
                tun = builder.establish();
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        engine.Key key = new engine.Key();
        key.setMark(0);
        key.setMTU(0);
        key.setDevice("fd://" + tun.getFd());
        key.setInterface("");
        key.setLogLevel("debug");
        key.setProxy(SOCKS5);
        key.setRestAPI("");
        key.setTCPSendBufferSize("");
        key.setTCPReceiveBufferSize("");
        key.setTCPModerateReceiveBuffer(false);

        engine.Engine.insert(key);
        executors.submit(Engine::start);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executors != null) {
            executors.shutdownNow();
        }
    }

    /**
     * Computes the inverted subnet, routing all traffic except to the specified subnet. Use prefixLength
     * of 32 or 128 for a single address.
     *
     * @see <a href="https://stackoverflow.com/a/41289228"></a>
     */
    private void addRoutesExcept(Builder builder, String address, int prefixLength) {
        try {
            byte[] bytes = InetAddress.getByName(address).getAddress();
            for (int i = 0; i < prefixLength; i++) { // each entry
                byte[] res = new byte[bytes.length];
                for (int j = 0; j <= i; j++) { // each prefix bit
                    res[j / 8] = (byte) (res[j / 8] | (bytes[j / 8] & (1 << (7 - (j % 8)))));
                }
                res[i / 8] ^= (1 << (7 - (i % 8)));

                builder.addRoute(InetAddress.getByAddress(res), i + 1);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
